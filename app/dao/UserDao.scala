package dao

import javax.inject.{Inject, Singleton}
import models.User
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

/**
  * @author dave.th
  * @date 2018. 9. 11.
  */
class Users(tag: Tag) extends Table[User](tag, "user") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def userName = column[Option[String]]("userName")
  def password = column[Option[String]]("password")
  def email = column[Option[String]]("email")

  override def * = (idx, userName, password, email) <> ((User.apply _).tupled, User.unapply)
}

@Singleton()
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  val users = TableQuery[Users]

  def getList = {
    db.run(users.result)
  }

  def userCheck(user: User) = {
    db.run(users.filter( users => users.email === user.email && users.password === user.password).result.headOption)
  }
}
