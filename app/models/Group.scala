package models

import play.api.libs.json.Json

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
case class Group(idx: Option[Int], name: Option[String])
object Group {
  implicit val reads = Json.reads[Group]
  implicit val writes = Json.writes[Group]
}
