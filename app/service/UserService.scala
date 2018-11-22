package service

import java.util.UUID

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import javax.inject.{Inject, Singleton}
import models.{User, UserDataAccess}
import org.mindrot.jbcrypt.BCrypt
import redis.clients.jedis.{Jedis, JedisPool}
import utils.MailUtils

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 08/10/2018
  */
@Singleton
class UserService @Inject()(private val userDataAccess: UserDataAccess,
                            private val jedisPool: JedisPool,
                            private val mailUtils: MailUtils,
                            implicit val ec: ExecutionContext) {
  val REDIS_CHANGE_PASSWORD_USER_IDX_KEY = "deTodoBack:changePasswordUser:%s"
  val CHANGE_PASSWORD_URL = "http://localhost:8080/#/changePassword/%s" // 설정으로 뺄 예정
  val CHANGE_PASSWORD_SUBJECT = "<우리 오늘 뭐해?> 비밀번호 재설정"

  def searchUser(keyword: String) = {
    userDataAccess.findByEmailOrUserName(keyword)
  }

  def save(user: User) = {
    userDataAccess.passwordUpdate(user)
  }

  def checkEmail(email: String) = {
    userDataAccess.findByEmail(email)
  }

  def sendChangePasswordMail(email: String) = {
    checkEmail(email) map { users =>
      users.headOption.fold(throw new RuntimeException("dd")) { user =>
        val randomCode = getChangeCode(user)
        val changePasswordLink = String.format(CHANGE_PASSWORD_URL, randomCode)
        mailUtils.sendMail(email, CHANGE_PASSWORD_SUBJECT, views.html.changePassword(changePasswordLink).toString())
      }
    }
  }

  def getChangePasswordUser(changeCode: String) = {
    val userIdx = getChangeUserIdx(changeCode)
    userDataAccess.findByIdx(userIdx.toInt)
  }

  def getChangeCode(user: User) = {
    val changeCode = UUID.randomUUID().toString.replaceAll("-", "")

    var jedis: Option[Jedis] = None
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper //TODO: json utils로 따로 뺄 예정
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      jedis.get.set(String.format(REDIS_CHANGE_PASSWORD_USER_IDX_KEY, changeCode), user.idx.get.toString)
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }

    changeCode
  }

  def getChangeUserIdx(changeCode:String) = {
    var jedis: Option[Jedis] = None
    var userIdx:String = ""
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper //TODO: json utils로 따로 뺄 예정
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

      Option(jedis.get.get(String.format(REDIS_CHANGE_PASSWORD_USER_IDX_KEY, changeCode))) map { jsonString =>
        userIdx = jsonString
      }
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }

    userIdx
  }
}
