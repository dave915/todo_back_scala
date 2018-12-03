package service

import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.{User, UserDataAccess}
import utils.{JedisUtils, MailUtils}

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 08/10/2018
  */
@Singleton
class UserService @Inject()(private val userDataAccess: UserDataAccess,
                            private val jedisUtils: JedisUtils,
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
      if(users.isEmpty)
        throw new NoSuchElementException("No Such Email.")

      val user = users.head
      val randomCode = getChangeCode(user)
      val changePasswordLink = String.format(CHANGE_PASSWORD_URL, randomCode)
      mailUtils.sendMail(Seq(email), CHANGE_PASSWORD_SUBJECT, views.html.changePassword(changePasswordLink).toString())
    }
  }

  def getChangePasswordUser(changeCode: String) = {
    val userIdx = getChangeUserIdx(changeCode)
    userDataAccess.findByIdx(userIdx.toInt)
  }

  def getChangeCode(user: User) = {
    val changeCode = UUID.randomUUID().toString.replaceAll("-", "")
    jedisUtils.set(String.format(REDIS_CHANGE_PASSWORD_USER_IDX_KEY, changeCode), user.idx.get.toString)

    changeCode
  }

  def getChangeUserIdx(changeCode:String) = {
    var userIdx:String = ""
    jedisUtils.get(String.format(REDIS_CHANGE_PASSWORD_USER_IDX_KEY, changeCode)) foreach { str =>
      userIdx = str
    }

    userIdx
  }
}
