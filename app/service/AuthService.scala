package service

import javax.inject.{Inject, Singleton}
import models.{GroupDataAccess, JoinGroupDataAccess, User, UserDataAccess}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class AuthService @Inject()(private val userDataAccess: UserDataAccess,
                            private val groupDataAccess: GroupDataAccess,
                            private val joinGroupDataAccess: JoinGroupDataAccess,
                            implicit val ec: ExecutionContext){

  def signIn(user: User): Unit = {
    userDataAccess.signIn(user).onComplete { newUser =>
      groupDataAccess.insert(newUser.get.email.get, true).onComplete { group =>
        joinGroupDataAccess.insert(group.get.idx.get, newUser.get.idx.get, 1)
      }
    }
  }

  def userCheck(user: User): Future[Option[User]] = {
    userDataAccess.userCheck(user)
  }
}
