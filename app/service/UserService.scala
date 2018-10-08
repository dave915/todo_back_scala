package service

import javax.inject.{Inject, Singleton}
import models.UserDataAccess

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 08/10/2018
  */
@Singleton
class UserService @Inject()(private val userDataAccess: UserDataAccess,
                            implicit val ec: ExecutionContext) {
  def searchUser(keyword: String) = {
    userDataAccess.findByEmailOrUserName(keyword)
  }
}
