package models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models.conversions.LocalDateTimeTableConversions._
import play.api.libs.functional.syntax._

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class ItemDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                        implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val items = TableQuery[Items]

  def save(item: Item): Future[Int] = {
    db.run(items insertOrUpdate item)
  }

  def delete(idx: Int): Future[Int] = {
    db.run(items.filter(_.idx === idx).delete)
  }

  def getItemListBySearchOption(itemSearchOption: ItemSearchOption): Future[Seq[Item]] = {
    db.run(mappingSearchOption(itemSearchOption).result)
  }

  private def mappingSearchOption(itemSearchOption: ItemSearchOption) : Query[Items, Items#TableElementType, Seq] = {
    var query = items.filter(_.groupIdx inSet itemSearchOption.groupIdxList.get)

    query = itemSearchOption.itemType.fold(query)(itemType => query.filter(_.`type` === itemType))

    query = itemSearchOption.status.fold(query)(status => query.filter(_.status === status))

    query = itemSearchOption.startDate.fold(query) { startDate =>
      val endDate : LocalDateTime = itemSearchOption.endDate.fold(startDate)(endDate => endDate)
      query.filter { items =>
          items.itemDatetime >= startDate && items.itemDatetime < endDate.plusDays(1)
        }
    }

    query = itemSearchOption.keyword.fold(query) { keyword =>
      val likeKeyword = "%" + keyword + "%"
      itemSearchOption.keywordType match {
        case Some(1) => query.filter { items =>
          items.itemTag.like(likeKeyword) || items.title.like(likeKeyword) || items.memo.like(likeKeyword)
        }
        case Some(2) => query.filter(_.itemTag.like(likeKeyword))
        case Some(3) => query.filter { items =>
          items.title.like(likeKeyword) || items.memo.like(likeKeyword)
        }
        case _ => query
      }
    }

    query
  }
}

case class Item(idx: Option[Int], groupIdx: Option[Int], title: Option[String], status: Option[Int],
                memo: Option[String], tag: Option[String], `type`: Option[Int], repeatType: Option[Int],
                itemDatetime: Option[LocalDateTime], createAt: Option[LocalDateTime])
object Item {
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
  implicit val writes: OWrites[Item] = Json.writes[Item]

  implicit val reads: Reads[Item] = (
    (JsPath \ "idx").readNullable[Int] and
      (JsPath \ "groupIdx").readNullable[Int] and
      (JsPath \ "title").readNullable[String] and
      (JsPath \ "status").readWithDefault[Int](1) and
      (JsPath \ "memo").readNullable[String] and
      (JsPath \ "tag").readNullable[String] and
      (JsPath \ "type").readWithDefault[Int](1) and
      (JsPath \ "repeatType").readWithDefault[Int](0) and
      (JsPath \ "itemDatetime").readNullable[String] and
      (JsPath \ "createAt").readNullable[String]
  )((idx: Option[Int], groupIdx: Option[Int], title: Option[String], status: Int,
     memo: Option[String], tag: Option[String], `type`: Int, repeatType: Int,
     itemDatetime: Option[String], createAt: Option[String]) =>
    Item(idx, groupIdx, title, Option(status), memo, tag, Option(`type`),
      Option(repeatType), itemDatetime.map(i => LocalDateTime.parse(i, dateTimeFormatter)), createAt.map(i => LocalDateTime.parse(i, dateTimeFormatter))))
}

class Items(tag: Tag) extends Table[Item](tag, "item") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def groupIdx = column[Option[Int]]("groupIdx")
  def title = column[Option[String]]("title")
  def status = column[Option[Int]]("status")
  def memo = column[Option[String]]("memo")
  def itemTag = column[Option[String]]("tag")
  def `type` = column[Option[Int]]("type")
  def repeatType = column[Option[Int]]("repeatType")
  def itemDatetime = column[Option[LocalDateTime]]("itemDatetime")
  def createAt = column[Option[LocalDateTime]]("createAt")

  override def * = (idx, groupIdx, title, status, memo, itemTag, `type`, repeatType, itemDatetime, createAt) <>
    ((Item.apply _).tupled, Item.unapply)
}
