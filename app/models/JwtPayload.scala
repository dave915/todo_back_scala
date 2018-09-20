package models

import java.util.Date

import play.api.libs.json.{Json, OWrites, Reads}

/**
  * @author dave.th
  * @date 2018. 9. 10.
  */
case class JwtPayload(idx: Option[Int], userName: Option[String], email: Option[String], exp: Date)

object JwtPayload {
  implicit val reads: Reads[JwtPayload] = Json.reads[JwtPayload]
  implicit val writes: OWrites[JwtPayload] = Json.writes[JwtPayload]
}
