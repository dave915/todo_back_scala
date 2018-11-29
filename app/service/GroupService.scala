package service

import java.util.UUID

import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.Json
import utils.{JedisUtils, MailUtils}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupService @Inject()(private val groupDataAccess: GroupDataAccess,
                             private val joinGroupDataAccess: JoinGroupDataAccess,
                             private val userDataAccess: UserDataAccess,
                             private val jedisUtils: JedisUtils,
                             private val mailUtils: MailUtils,
                             implicit val ec: ExecutionContext) {
  val REDIS_INVITE_GROUP_TOKEN_KEY = "deTodoBack:inviteGroupToken:%s"
  val GROUP_INVITE_URL = "http://localhost:8080/#/invite/group/%s" // 설정으로 뺄 예정
  val GROUP_INVITE_SUBJECT = "<우리 오늘 뭐해?> 그룹초대"

  def getJoinGroupByUserIdx(idx: Int): Future[Seq[GroupInfo]] = {
    groupDataAccess.getJoinGroupByUserIdx(idx) map { group =>
      group.map(groupInfo => {
        GroupInfo(groupInfo._1.idx.get, groupInfo._1.name.get, groupInfo._1.isDefaultGroup.get, groupInfo._2)
      })
    }
  }

  def addGroup(group: Group, idx: Int) = {
    groupDataAccess.insert(group.name.get, isDefaultGroup = false) map { group =>
      joinGroupDataAccess.save(JoinGroup(group.idx.get, idx, 1))
    }
  }

  def updateGroup(group: Group) = {
    groupDataAccess.update(group)
  }

  def getJoinUsers(groupIdx: Int) = {
    joinGroupDataAccess.getJoinUsers(groupIdx).map { list =>
      list map { tuple =>
        (tuple._1, tuple._2.`type`)
      }
    }
  }

  def inviteUser(groupIdx: Int, userIdx: Int, requestUserId: Int) = {
    for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, requestUserId)
      if currentUser.get.`type` == 1
      joinUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
      result <- inviteGroup(joinUser, userIdx, groupIdx)
    } yield result
  }

  def inviteGroup(joinGroup: Option[JoinGroup], userIdx: Int, groupIdx: Int) = {
    joinGroup match {
      case None => {
        val inviteInfo = for {
          user <- userDataAccess.findByIdx(userIdx)
          group <- groupDataAccess.findByIdx(groupIdx)
        } yield (user, group)

        inviteInfo map { info =>
          try {
            val inviteCode = getInviteCode(JoinGroup(groupIdx, info._1.idx.get, 2))
            val inviteConfirmLink = String.format(GROUP_INVITE_URL, inviteCode)
            mailUtils.sendMail(Seq(info._1.email.get), GROUP_INVITE_SUBJECT, views.html.groupInvite(info._2.name.get, inviteConfirmLink).toString())
            joinGroupDataAccess.save(JoinGroup(groupIdx, info._1.idx.get, 3))
          } catch {
            case e : Exception =>
              throw new RuntimeException("Please check email")
          }
        }
      }
      case _ => throw new RuntimeException("Already group user")
    }
  }

  def checkInvite(inviteCode: String) = {
    checkInviteCode(inviteCode).fold {
      throw new RuntimeException("Unidentifiable code")
    } { joinGroup =>
      joinGroupDataAccess.save(joinGroup)
    }
  }

  def joinGroup(groupIdx: Int, userIdx: Int) = {
    joinGroupDataAccess.save(JoinGroup(groupIdx, userIdx, 2))
  }

  def passGroupOwner(groupIdx: Int, userIdx: Int, ownerIdx: Int) = {
    joinGroupDataAccess.save(JoinGroup(groupIdx, userIdx, 1))
    joinGroupDataAccess.save(JoinGroup(groupIdx, ownerIdx, 2))
  }

  def leaveGroup(groupIdx: Int, userIdx: Int) = {
    val newOwner: Future[User] = for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
      if currentUser.get.`type` == 1
      users <- joinGroupDataAccess.getJoinUsers(groupIdx)
    } yield users.filter(!_._1.idx.equals(Option(userIdx))).minBy(_._2.createAt.toString)._1

    newOwner.map(user => joinGroupDataAccess.save(JoinGroup(groupIdx, user.idx.get, 1)))
    joinGroupDataAccess.delete(groupIdx, userIdx)
  }

  def banishUser(groupIdx: Int, userIdx: Int, requestUserId: Int) = {
    for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, requestUserId)
      if currentUser.get.`type` == 1
      result <- joinGroupDataAccess.delete(groupIdx, userIdx)
    } yield result
  }

  def getInviteCode(joinInfo: JoinGroup): String = {
    val randomToken = UUID.randomUUID().toString.replaceAll("-", "")
    jedisUtils.set(String.format(REDIS_INVITE_GROUP_TOKEN_KEY, randomToken), joinInfo)

    randomToken
  }

  def checkInviteCode(inviteCode: String) = {
    var joinGroup:JoinGroup = null
    Option(jedisUtils.get(String.format(REDIS_INVITE_GROUP_TOKEN_KEY, inviteCode))) foreach { str =>
      joinGroup = Json.parse(str).as[JoinGroup]
    }
    Option(joinGroup)
  }
}
