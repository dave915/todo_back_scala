package controllers

import javax.inject.{Inject, Singleton}
import models.User
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.UserService

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author dave.th
  * @date 08/10/2018
  */
@Singleton
class UserController @Inject()(implicit val auth: SecuredAuthenticator,
                               implicit val userService: UserService,
                               implicit val cc: ControllerComponents,
                               implicit val ec: ExecutionContext) extends AbstractController(cc){
  def searchUser(keyword: String) = auth.JWTAuthentication.async { implicit request =>
    userService.searchUser(keyword) map { users =>
      Ok(Json.toJson(users))
    }
  }

  def save = Action.async { implicit request =>
    val user = request.body.asJson.get.as[User]

    userService.save(user)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def sendChangePasswordMail = Action.async { implicit request =>
    var email = ""
    request.queryString.get("email").foreach(seq => email = seq.head)

    userService.sendChangePasswordMail(email)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def getChangePasswordUser(changeCode: String) = Action.async { implicit request =>
    userService.getChangePasswordUser(changeCode) map { user =>
      Ok(Json.toJson(user))
    }
  }
}
