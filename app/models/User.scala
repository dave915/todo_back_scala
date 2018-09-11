package models

import play.api.libs.json._

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
case class User(idx: Option[Int], userName: Option[String], password: Option[String], email: Option[String])
object User {
  implicit val reads = Json.reads[User]
  implicit val writes = Json.writes[User]
}
