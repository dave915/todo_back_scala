package controllers

import javax.inject.{Inject, Singleton}
import models.User
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

  def save = Action { implicit request =>
    val user = request.body.asJson.get.as[User]

    try {
      userService.save(user)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def sendChangePasswordMail = Action { implicit request =>
    var email = ""
    request.queryString.get("email").foreach(seq => email = seq.head)

    try {
      userService.sendChangePasswordMail(email)
      Ok(Json.toJson(Map("result" -> "success"))) //TODO: 오류 체크
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def getChangePasswordUser(changeCode: String) = Action.async { implicit request =>
    userService.getChangePasswordUser(changeCode) map { user =>
      Ok(Json.toJson(user))
    }
  }
}
