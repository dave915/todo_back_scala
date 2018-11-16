package controllers

import javax.inject.{Inject, Singleton}
import models.User
import play.api.libs.json.Json
import play.api.mvc._
import service.AuthService
import utils.SendMailSMTP

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
@Singleton
class AuthController @Inject()(auth: SecuredAuthenticator,
                               authService: AuthService,
                               cc: ControllerComponents,
                               sendMailSMTP: SendMailSMTP,
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

  def login = Action.async { implicit request =>
    val user = request.body.asJson.get.as[User]

    authService.userCheck(user) map { user =>
      if (user.isEmpty) {
        Unauthorized(Json.toJson(Map("result" -> "fail")))
      } else {
        val userInfo = user.get
        auth.setUserRefreshToken(userInfo.idx.get)
        Ok(Json.toJson(Map("result" -> "success"))).withCookies(
          Cookie("jwt_token", auth.makeNewJwtToken(userInfo))
        )

      }
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

  def mailTest = auth.JWTAuthentication{ implicit  request =>

    sendMailSMTP.sendMail("dave.th@kakaocorp.com", "<우리 오늘 뭐해?> 그룹초대", views.html.groupInvite("텍스트11", "http://www.naver.com").toString())
    Ok(Json.toJson("success"))
  }
}
