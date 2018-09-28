package models

import java.time.LocalDate

import play.api.libs.json.{Json, OWrites, Reads}

/**
  * @author dave.th
  * @date 28/09/2018
  */
case class ItemSearchOption(startDate: Option[LocalDate], endDate: Option[LocalDate], groupIdx: Option[Int],
                            keywordType: Option[Int], keyword: Option[String], itemType: Option[Int])
object ItemSearchOption {
  implicit val reads: Reads[ItemSearchOption] = Json.reads[ItemSearchOption]
  implicit val writes: OWrites[ItemSearchOption] = Json.writes[ItemSearchOption]
}
