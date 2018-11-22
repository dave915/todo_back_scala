package models

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.{ExecutionContext, Future}

import models.conversions.LocalDateTimeTableConversions._

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
@Singleton()
class UserDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                        implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val users = TableQuery[Users]

  def signIn(user: User): Future[User] = {
    val query = (users returning users.map(_.idx) into((user, idx) => user.copy(idx = idx))) +=
      user.copy(password = Some(BCrypt.hashpw(user.password.get, BCrypt.gensalt())), createAt = Some(LocalDateTime.now()))

    db.run(query)
  }

  def save(user: User): Future[Int] = {
    db.run(users.filter(_.idx === user.idx).update(user))
  }

  def passwordUpdate(user: User): Future[Int] = {
    val q = for { u <- users if u.idx === user.idx.get } yield u.password
    db.run(q.update(Some(BCrypt.hashpw(user.password.get, BCrypt.gensalt()))))
  }

  def getList: Future[Seq[User]] = {
    db.run(users.result)
  }

  def userCheck(user: User): Future[Option[User]] = {
    db.run {
      users.filter{_.email === user.email}.result.headOption
    } map {
      case Some(u) if BCrypt.checkpw(user.password.get, u.password.get) => Some(u)
      case _ => None
    }
  }

  def findByIdx(idx: Int): Future[User] = {
    db.run(users.filter(_.idx === idx).result.head)
  }

  def findByEmailOrUserName(keyword: String) = {
    val likeKeyword = "%" + keyword + "%"
    db.run(users.filter(users => users.email.like(likeKeyword) || users.userName.like(likeKeyword)).result)
  }

  def findByEmail(email: String) = {
    db.run(users.filter(users => users.email === email).result)
  }
}

case class User(idx: Option[Int], userName: Option[String], password: Option[String], email: Option[String], refreshToken: Option[String], createAt: Option[LocalDateTime])

object User {
  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: Writes[User] = (
    (JsPath \ "idx").write[Option[Int]] and
      (JsPath \ "userName").write[Option[String]] and
      (JsPath \ "email").write[Option[String]]
  )(user => (user.idx, user.userName, user.email))
}

class Users(tag: Tag) extends Table[User](tag, "user") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def userName = column[Option[String]]("userName")
  def password = column[Option[String]]("password")
  def email = column[Option[String]]("email")
  def refreshToken = column[Option[String]]("refreshToken")
  def createAt = column[Option[LocalDateTime]]("createAt")

  override def * = (idx, userName, password, email, refreshToken, createAt) <> ((User.apply _).tupled, User.unapply)
}
