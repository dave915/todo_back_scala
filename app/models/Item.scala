package models

import java.sql.Date

import play.api.libs.json.Json

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
case class Item(idx: Option[Int], groupIdx: Option[Int], title: Option[String], status: Option[Int],
                memo: Option[String], tag: Option[String], `type`: Option[Int],
                startDateTime: Option[Date], endDateTime: Option[Date])
object Item {
  implicit val reads = Json.reads[Item]
  implicit val writes = Json.writes[Item]
}
