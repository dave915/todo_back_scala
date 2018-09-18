package controllers

import java.util.{Date, UUID}

import dao.UserDao
import javax.inject.Inject
import models.{JwtPayload, User}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import utils.JwtUtils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class UserRequest[A](user: User, request: Request[A]) extends WrappedRequest(request)

class SecuredAuthenticator @Inject()(cc: ControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     userDao: UserDao) extends AbstractController(cc) {
  implicit val formatUserDetails: OFormat[User] = Json.format[User]

  def makeNewJwtToken(userInfo: User): String = {
    val expireTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5분
    val payload = JwtPayload(userInfo.idx, userInfo.userName, userInfo.email, new Date(expireTime))
    JwtUtils.createToken(Json.toJson(payload).toString())
  }

  def makeNewRefreshToken() = {
    val expireTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30분
    val refreshToken = UUID.randomUUID() + "-" + expireTime
    refreshToken
  }

  def setUserRefreshToken(userIdx: Int): Unit = {
    val refreshToken = makeNewRefreshToken()
    userDao.findByIdx(userIdx) map { user =>
      userDao.save(user.copy(refreshToken = Some(refreshToken)))
    }

    // redis 저장
  }

  object JWTAuthentication extends ActionBuilder[UserRequest, AnyContent] {
    def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      val jwtToken = request.cookies.get("jwt_token").fold("")(cookie => cookie.value)

      if (!JwtUtils.isValidToken(jwtToken)) {
        return Future.successful(Unauthorized("Invalid credential"))
      }

      JwtUtils.decodePayload(jwtToken).fold(Future.successful(Unauthorized("Invalid credential"))) { payload =>
        val jstPayload = Json.parse(payload).validate[JwtPayload].get

        // token 만료, 리플래쉬 처리
        var newJwtToken: Option[String] = None
        if (isExpireToken(jstPayload.exp)) {
          newJwtToken = getNewToken(jstPayload.idx.get)
          if(newJwtToken.isEmpty)
            return Future.successful(Unauthorized("Invalid credential"))
        }

        val result = block(UserRequest(User(jstPayload.idx, jstPayload.userName, None, jstPayload.email, None), request))
        newJwtToken.fold(result) { token =>
          result map { futureResult =>
            futureResult.withCookies(
              Cookie("jwt_token", token)
            )
          }
        }
      }
    }

    def isExpireToken(tokenExpireDate: Date): Boolean = {
      val current = new Date(System.currentTimeMillis())
      current.after(tokenExpireDate)
    }

    def getNewToken(userIdx: Int): Option[String] = {
      var refreshToken: Option[String] = None

      // redis 조회

      val makeNewToken = userDao.findByIdx(userIdx) map { user =>
        user.refreshToken.fold(){ token =>
          val expireTime = token.split("-")(5).toLong
          if(!isExpireToken(new Date(expireTime)))
            refreshToken = Some(makeNewJwtToken(user))

          extendRefreshToken(user, expireTime)
        }
      }

      Await.result(makeNewToken, Duration.Inf)
      refreshToken
    }

    def extendRefreshToken(user: User, expireTime: Long): Unit = {
      val validTerm = 10 * 60 * 1000 // 연장 유효 시간차 10분
      val expireTimeTerm = expireTime - System.currentTimeMillis() // 남은 만료시간

      // refreshToken 만료시간이 0 ~ 10분 사이면 갱신
      if(expireTimeTerm >= 0 && expireTimeTerm <= validTerm)
        setUserRefreshToken(user.idx.get)
    }

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
    override protected def executionContext: ExecutionContext = cc.executionContext
  }

}
