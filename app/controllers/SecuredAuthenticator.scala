package controllers

import java.util.{Date, UUID}

import javax.inject.Inject
import models.{JwtPayload, User, UserDataAccess}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import utils.{JedisUtils, JwtUtils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class UserRequest[A](user: User, request: Request[A]) extends WrappedRequest(request)

class SecuredAuthenticator @Inject()(private val cc: ControllerComponents,
                                     private val userDataAccess: UserDataAccess,
                                     private val jedisUtils: JedisUtils,
                                     implicit val ec: ExecutionContext) extends AbstractController(cc) {

  implicit val formatUserDetails: OFormat[User] = Json.format[User]

  val REDIS_REFRESH_TOKEN_KEY = "deTodoBack:refreshToken:%s"
  val REFRESH_TOKEN_EXPIRE_TIME_MILLIS: Int = 30 * 60 * 1000 // 30분
  val JWT_TOKEN_EXPIRE_TIME_MILLIS: Int = 5 * 60 * 1000 // 5분
  val JWT_TOKEN_VALID_TERM_TIME_MILLIS: Int = 10 * 60 * 1000 // 연장 유효 시간차 10분

  def setUserRefreshToken(userIdx: Int): Unit = {
    val expireTime = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME_MILLIS // 30분
    val refreshToken = makeNewRefreshToken(expireTime)
    userDataAccess.findByIdx(userIdx) map { user =>
      userDataAccess.save(user.copy(refreshToken = Some(refreshToken)))
    }

    // redis 저장
    jedisUtils.setWithExpire(
      String.format(REDIS_REFRESH_TOKEN_KEY, userIdx.toString),
      refreshToken,
      REFRESH_TOKEN_EXPIRE_TIME_MILLIS / 1000
    )
  }

  def makeNewRefreshToken(expireTime: Long): String = {
    val refreshToken = UUID.randomUUID() + "-" + expireTime
    refreshToken
  }

  def makeNewJwtToken(userInfo: User): String = {
    val expireTime = System.currentTimeMillis() + JWT_TOKEN_EXPIRE_TIME_MILLIS
    val payload = JwtPayload(userInfo.idx, userInfo.userName, userInfo.email, new Date(expireTime))
    JwtUtils.createToken(Json.toJson(payload).toString())
  }

  def isExpireToken(tokenExpireDate: Date): Boolean = {
    val current = new Date(System.currentTimeMillis())
    current.after(tokenExpireDate)
  }

  def getNewToken(jwtPayload: JwtPayload): Option[String] = {
    var refreshToken: Option[String] = None
    var orgRefreshToken: Option[String] = None

    orgRefreshToken = jedisUtils.get(String.format(REDIS_REFRESH_TOKEN_KEY, jwtPayload.idx.get.toString))
      .fold(getDbRefreshToken(jwtPayload)) { token => Some(token) }

    if(orgRefreshToken.isDefined) {
      val expireTime = orgRefreshToken.get.replaceAll("\"", "").split("-")(5).toLong
      val user = User(jwtPayload.idx, jwtPayload.userName, None, jwtPayload.email, None, None)
      if (!isExpireToken(new Date(expireTime)))
        refreshToken = Some(makeNewJwtToken(user))

      extendRefreshToken(user, expireTime)
    }

    refreshToken
  }

  def getDbRefreshToken(jwtPayload: JwtPayload): Option[String] = {
    var orgRefreshToken: Option[String] = None
    val makeNewToken = userDataAccess.findByIdx(jwtPayload.idx.get) map { user =>
      user.refreshToken foreach { token => orgRefreshToken = Some(token)}
    }
    Await.result(makeNewToken, Duration.Inf)
    orgRefreshToken
  }

  def extendRefreshToken(user: User, expireTime: Long): Unit = {
    val expireTimeTerm = expireTime - System.currentTimeMillis() // 남은 만료시간

    // refreshToken 만료시간이 0 ~ 10분 사이면 갱신
    if (expireTimeTerm >= 0 && expireTimeTerm <= JWT_TOKEN_VALID_TERM_TIME_MILLIS)
      setUserRefreshToken(user.idx.get)
  }

  object JWTAuthentication extends ActionBuilder[UserRequest, AnyContent] {
    def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      val jwtToken = request.cookies.get("jwt_token").fold("")(cookie => cookie.value)

      if (!JwtUtils.isValidToken(jwtToken)) {
        return Future.successful(Unauthorized("Invalid credential"))
      }

      JwtUtils.decodePayload(jwtToken).fold(Future.successful(Unauthorized("Invalid credential"))) { payload =>
        val jwtPayload = Json.parse(payload).validate[JwtPayload].get

        // token 만료, 리플래쉬 처리
        var newJwtToken: Option[String] = None
        if (isExpireToken(jwtPayload.exp)) {
          newJwtToken = getNewToken(jwtPayload)
          if (newJwtToken.isEmpty)
            return Future.successful(Unauthorized("Invalid credential"))
        }

        val result = block(UserRequest(User(jwtPayload.idx, jwtPayload.userName, None, jwtPayload.email, None, None), request))
        newJwtToken.fold(result) { token =>
          result map { futureResult =>
            futureResult.withCookies(
              Cookie("jwt_token", token)
            )
          }
        }

      }
    }

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext
  }

}
