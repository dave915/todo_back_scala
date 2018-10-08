package controllers

import javax.inject.Inject
import models.Group
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.GroupService

import scala.concurrent.ExecutionContext

class GroupController @Inject()(auth: SecuredAuthenticator,
                                groupService: GroupService,
                                cc: ControllerComponents,
                                implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def getGroupByUserIdx = auth.JWTAuthentication.async { implicit request =>
    val userIdx = request.user.idx.get

    groupService.getJoinGroupByUserIdx(userIdx) map { seq =>
      Ok(Json.toJson(seq))
    }
  }

  def addGroup = auth.JWTAuthentication { implicit  request =>
    val group = request.body.asJson.get.as[Group]
    val userIdx = request.user.idx.get

    try {
      groupService.addGroup(group, userIdx)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def getJoinUsers(groupIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.getJoinUsers(groupIdx) map { users =>
      Ok(Json.toJson(users))
    }
  }
}
