package service

import dao.{GroupDao, JoinGroupDao, UserDao}
import javax.inject.{Inject, Singleton}
import models.User

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class AuthService @Inject()(private val userDao: UserDao,
                            private val groupDao: GroupDao,
                            private val joinGroupDao: JoinGroupDao,
                            implicit val ec: ExecutionContext){

  def signIn(user: User) = {
    userDao.signIn(user).onComplete { newUser =>
      groupDao.insert(newUser.get.email.get, true).onComplete { group =>
        joinGroupDao.insert(group.get.idx.get, newUser.get.idx.get, 1)
      }
    }
  }

  def userCheck(user: User) = {
    userDao.userCheck(user)
  }
}
