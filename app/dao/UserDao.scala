package dao

import javax.inject.{Inject, Singleton}
import models.User
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 11.
  */
class Users(tag: Tag) extends Table[User](tag, "user") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def userName = column[Option[String]]("userName")
  def password = column[Option[String]]("password")
  def email = column[Option[String]]("email")
  def refreshToken = column[Option[String]]("refreshToken")

  override def * = (idx, userName, password, email, refreshToken) <> ((User.apply _).tupled, User.unapply)
}

@Singleton()
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                        implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val users = TableQuery[Users]

  def signIn(user: User) = {
    val query = (users returning users.map(_.idx) into((user, idx) => user.copy(idx = idx))) +=
      user.copy(password = Some(BCrypt.hashpw(user.password.get, BCrypt.gensalt())))

    db.run(query)
  }

  def save(user: User) = {
    db.run(users.filter(_.idx === user.idx).update(user))
  }

  def getList = {
    db.run(users.result)
  }

  def userCheck(user: User) = {
    db.run {
      users.filter{_.email === user.email}.result.headOption
    } map {
      case Some(u) if BCrypt.checkpw(user.password.get, u.password.get) => Some(u)
      case _ => None
    }
  }

  def findByIdx(idx: Int) = {
    db.run(users.filter(_.idx === idx).result.head)
  }
}
