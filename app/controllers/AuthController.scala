package controllers

import javax.inject.{Inject, Singleton}
import models.User
import play.api.libs.json.Json
import play.api.mvc._
import service.AuthService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
@Singleton
class AuthController @Inject()(auth: SecuredAuthenticator,
                               authService: AuthService,
                               cc: ControllerComponents,
                               implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def signUp = Action { implicit request =>
    val user = request.body.asJson.get.as[User]

    Try(authService.signUp(user)) match {
      case Success(_) => Ok(Json.obj("result" -> "success"))
      case Failure(_) => BadRequest(Json.obj("result" -> "fail"))
    }
  }

  def login = Action.async { implicit request =>
    val user = request.body.asJson.get.as[User]

    authService.userCheck(user) map {
      case null => Unauthorized(Json.obj("result" -> "fail"))
      case u : User => auth.setUserRefreshToken(u.idx.get)
        Ok(Json.obj("result" -> "success")).withCookies(
          Cookie("jwt_token", auth.makeNewJwtToken(u))
        )
    }
  }

  def logout = Action {
    Ok("logout").withCookies(
      Cookie("jwt_token", "")
    )
  }

  def currentUserInfo = auth.JWTAuthentication{ implicit request =>
    Ok(Json.toJson(request.user))
  }
}
