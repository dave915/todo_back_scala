package models

import play.api.libs.json.{Json, OWrites, Reads}

case class GroupInfo(idx: Int, name: String, isDefaultGroup: Boolean, role: Int)
object GroupInfo {
  implicit val reads: Reads[GroupInfo] = Json.reads[GroupInfo]
  implicit val writes: OWrites[GroupInfo] = Json.writes[GroupInfo]
}
