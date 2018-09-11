package controllers

import dao.UserDao
import javax.inject.{Inject, Singleton}
import models.{JwtPayload, User}
import play.api.libs.json.Json
import play.api.mvc._
import utils.JwtUtils

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
@Singleton
class UserController @Inject()(auth: SecuredAuthenticator, userDao: UserDao, cc: ControllerComponents, implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def login: Action[AnyContent] = Action.async { implicit request =>
    val user = request.body.asJson.get.as[User]

    userDao.userCheck(user) map { user =>
      if (user.isEmpty) {
        Unauthorized("login fail")
      } else {
        val userInfo = user.get
        Ok(Json.toJson(Map("result" -> "success"))).withCookies(
          Cookie("jw_token", JwtUtils.createToken(Json.toJson(JwtPayload(userInfo.idx, userInfo.userName, userInfo.email)).toString()))
        )
      }
    }
  }

  def logout = Action {
    Ok("logout").discardingCookies(DiscardingCookie("jw_token"))
  }

  def currentUserInfo = auth.JWTAuthentication{ implicit request =>
    Ok(Json.toJson(request.user))
  }
}
