package models

import play.api.libs.json.Json

case class GroupInfo(idx: Int, name: String, isDefaultGroup: Boolean, role: Int)
object GroupInfo {
  implicit val reads = Json.reads[GroupInfo]
  implicit val writes = Json.writes[GroupInfo]
}
