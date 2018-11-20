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

  def updateGroup = auth.JWTAuthentication { implicit request =>
    val group = request.body.asJson.get.as[Group]

    try {
      groupService.updateGroup(group)
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

  def inviteUser(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication { implicit request =>
    try {
      groupService.inviteUser(groupIdx, userIdx, request.user.idx.get)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def checkInvite(inviteCode: String) = Action { implicit request =>
    try {
      groupService.checkInvite(inviteCode)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def joinGroup(groupIdx: Int, userIdx:Int) = auth.JWTAuthentication { implicit request =>
    try {
      groupService.joinGroup(groupIdx, userIdx)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def passGroupOwner(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication { implicit request =>
    try {
      groupService.passGroupOwner(groupIdx, userIdx, request.user.idx.get)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def leaveGroup(groupIdx: Int) = auth.JWTAuthentication { implicit request =>
    try {
      groupService.leaveGroup(groupIdx, request.user.idx.get)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }

  def banishUser(groupIdx: Int, userIdx: Int) = auth.JWTAuthentication { implicit request =>
    try {
      groupService.banishUser(groupIdx, userIdx, request.user.idx.get)
      Ok(Json.toJson(Map("result" -> "success")))
    } catch {
      case e : Exception =>
        println(e)
        BadRequest(Json.toJson(Map("result" -> "fail")))
    }
  }
}
