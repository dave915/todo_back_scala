package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.UserService

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 08/10/2018
  */
@Singleton
class UserController @Inject()(auth: SecuredAuthenticator,
                               userService: UserService,
                               cc: ControllerComponents,
                               implicit val ec: ExecutionContext) extends AbstractController(cc){
  def searchUser(keyword: String) = auth.JWTAuthentication.async { implicit request =>
    userService.searchUser(keyword) map { users =>
      Ok(Json.toJson(users))
    }
  }
}
