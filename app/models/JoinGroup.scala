package models

import play.api.libs.json.Json

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
case class JoinGroup(groupIdx: Int, userIdx: Int, `type`: Int)
object JoinGroup {
  implicit val reads = Json.reads[JoinGroup]
  implicit val writes = Json.writes[JoinGroup]
}
