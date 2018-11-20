package service

import java.util.UUID

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import javax.inject.{Inject, Singleton}
import models._
import play.api.libs.json.Json
import redis.clients.jedis.{Jedis, JedisPool}
import utils.MailUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupService @Inject()(private val groupDataAccess: GroupDataAccess,
                             private val joinGroupDataAccess: JoinGroupDataAccess,
                             private val userDataAccess: UserDataAccess,
                             private val jedisPool: JedisPool,
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

  def addGroup(group: Group, idx: Int): Unit = {
    groupDataAccess.insert(group.name.get, isDefaultGroup = false).onComplete { group =>
      joinGroupDataAccess.save(JoinGroup(group.get.idx.get, idx, 1))
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
    val inviteUser = for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, requestUserId)
      if currentUser.`type` == 1
      joinUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
    } yield joinUser

    // TODO: 에러 메세지 안나감
    inviteUser.onComplete(result => {
      if (result.toOption.isEmpty) {
        val inviteInfo = for {
          user <- userDataAccess.findByIdx(userIdx)
          group <- groupDataAccess.findByIdx(groupIdx)
        } yield (user, group)

        inviteInfo map { info =>
          joinGroupDataAccess.save(JoinGroup(groupIdx, info._1.idx.get, 3))
          val inviteCode = getInviteCode(JoinGroup(groupIdx, info._1.idx.get, 2))
          val inviteConfirmLink = String.format(GROUP_INVITE_URL, inviteCode)
          mailUtils.sendMail(info._1.email.get, GROUP_INVITE_SUBJECT, views.html.groupInvite(info._2.name.get, inviteConfirmLink).toString())
        }
      } else {
        throw new RuntimeException("already group user")
      }
    })
  }

  def checkInvite(inviteCode: String) = {
    Option(checkInviteCode(inviteCode)) map { joinGroup =>
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

  def leaveGroup(groupIdx: Int, userIdx: Int): Unit = {
    val newOwner: Future[User] = for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
      if currentUser.`type` == 1
      users <- joinGroupDataAccess.getJoinUsers(groupIdx)
    } yield users.filter(!_._1.idx.equals(Option(userIdx))).minBy(_._2.createAt.toString)._1

    newOwner.map(user => joinGroupDataAccess.save(JoinGroup(groupIdx, user.idx.get, 1)))
    joinGroupDataAccess.delete(groupIdx, userIdx)
  }

  def banishUser(groupIdx: Int, userIdx: Int, requestUserId: Int): Unit = {
    for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, requestUserId)
      if currentUser.`type` == 1
      result <- joinGroupDataAccess.delete(groupIdx, userIdx)
    } yield result
  }

  def getInviteCode(joinInfo: JoinGroup): String = {
    val randomToken = UUID.randomUUID().toString.replaceAll("-", "")

    var jedis: Option[Jedis] = None
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper //TODO: json utils로 따로 뺄 예정
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      val objectString = objectMapper.writeValueAsString(joinInfo)
      jedis.get.set(String.format(REDIS_INVITE_GROUP_TOKEN_KEY, randomToken), objectString)
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }

    randomToken
  }

  def checkInviteCode(inviteCode: String) = {
    var jedis: Option[Jedis] = None
    var joinGroup:JoinGroup = null
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper //TODO: json utils로 따로 뺄 예정
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

      Option(jedis.get.get(String.format(REDIS_INVITE_GROUP_TOKEN_KEY, inviteCode))) map { jsonString =>
        joinGroup = Json.parse(jsonString).as[JoinGroup]
      }
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }

    joinGroup
  }
}
