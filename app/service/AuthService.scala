package service

import javax.inject.{Inject, Singleton}
import models._

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

  def signIn(user: User) = {
    for {
      newUser <- userDataAccess.signIn(user)
      group <- groupDataAccess.insert(newUser.email.get, isDefaultGroup = true)
    } yield joinGroupDataAccess.save(JoinGroup(group.idx.get, newUser.idx.get, 1))
  }

  def userCheck(user: User): Future[Option[User]] = {
    userDataAccess.userCheck(user)
  }
}
