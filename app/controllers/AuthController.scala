package controllers

import javax.inject.{Inject, Singleton}
import models.{JwtPayload, User}
import play.api.libs.json.Json
import play.api.mvc._
import service.AuthService
import utils.JwtUtils

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
@Singleton
class AuthController @Inject()(auth: SecuredAuthenticator,
                               authService: AuthService,
                               cc: ControllerComponents,
                               implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def signIn = Action { implicit request =>
    val user = request.body.asJson.get.as[User]

    try {
      authService.signIn(user)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def login: Action[AnyContent] = Action.async { implicit request =>
    val user = request.body.asJson.get.as[User]

    authService.userCheck(user) map { user =>
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
