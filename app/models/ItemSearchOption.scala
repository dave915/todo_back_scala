package models

import java.time.LocalDateTime

import play.api.libs.json.{Json, OWrites, Reads}

/**
  * @author dave.th
  * @date 28/09/2018
  */
case class ItemSearchOption(startDate: Option[LocalDateTime], endDate: Option[LocalDateTime],
                            status: Option[Int], groupIdx: Option[Int], groupIdxList: Option[List[Int]],
                            keywordType: Option[Int], keyword: Option[String], itemType: Option[Int])
object ItemSearchOption {
  implicit val reads: Reads[ItemSearchOption] = Json.reads[ItemSearchOption]
  implicit val writes: OWrites[ItemSearchOption] = Json.writes[ItemSearchOption]

  def convert(map: Map[String, String]): ItemSearchOption = {
    val startDate = map.get("startDate").map(i => LocalDateTime.parse(i))
    val endDate = map.get("endDate").map(i => LocalDateTime.parse(i))
    val status = map.get("status").map(i => i.toInt)
    val groupIdx = map.get("groupIdx").map(i => i.toInt)
    val keywordType = map.get("keywordType").fold(Some(1))(i => Some(i.toInt))
    val keyword = map.get("keyword")
    val itemType = map.get("itemType").fold(Some(1))(i => Some(i.toInt))

    ItemSearchOption(startDate, endDate, status, groupIdx, Option(List()), keywordType, keyword, itemType)
  }
}
