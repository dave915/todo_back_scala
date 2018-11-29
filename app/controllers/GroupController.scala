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

  def addGroup = auth.JWTAuthentication.async { implicit  request =>
    val group = request.body.asJson.get.as[Group]
    val userIdx = request.user.idx.get

    groupService.addGroup(group, userIdx)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def updateGroup = auth.JWTAuthentication.async { implicit request =>
    val group = request.body.asJson.get.as[Group]

    groupService.updateGroup(group)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def getJoinUsers(groupIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.getJoinUsers(groupIdx) map { users =>
      Ok(Json.toJson(users))
    }
  }

  def inviteUser(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.inviteUser(groupIdx, userIdx, request.user.idx.get)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def checkInvite(inviteCode: String) = Action.async { implicit request =>
    groupService.checkInvite(inviteCode)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def joinGroup(groupIdx: Int, userIdx:Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.joinGroup(groupIdx, userIdx)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def passGroupOwner(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.passGroupOwner(groupIdx, userIdx, request.user.idx.get)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def leaveGroup(groupIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.leaveGroup(groupIdx, request.user.idx.get)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def banishUser(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication.async { implicit request =>
    groupService.banishUser(groupIdx, userIdx, request.user.idx.get)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception =>
          println(e)
          BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }
}
