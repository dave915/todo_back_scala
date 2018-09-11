package controllers

import javax.inject.Inject
import models.{JwtPayload, User}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import utils.JwtUtils

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](user: User, request: Request[A]) extends WrappedRequest(request)

class SecuredAuthenticator @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit val formatUserDetails: OFormat[User] = Json.format[User]

  object JWTAuthentication extends ActionBuilder[UserRequest, AnyContent] {
    def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      val jwtToken = request.cookies.get("jw_token").fold("")(cookie => cookie.value)

      if (JwtUtils.isValidToken(jwtToken)) {
        JwtUtils.decodePayload(jwtToken).fold {
          Future.successful(Unauthorized("Invalid credential"))
        } { payload =>
          val userCredentials = Json.parse(payload).validate[JwtPayload].get

          // token 만료, 리플래쉬 처리

          block(UserRequest(User(userCredentials.idx, userCredentials.userName, None, userCredentials.email), request))
        }
      } else {
        Future.successful(Unauthorized("Invalid credential"))
      }
    }

    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext
  }

}
