package models

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}

import javax.inject.{Inject, Singleton}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, OWrites, Reads}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models.conversions.LocalDateTableConversions._

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
          items.itemDatetime >= startDate && items.itemDatetime <= endDate
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
  implicit val reads: Reads[Item] = Json.reads[Item]
  implicit val writes: OWrites[Item] = Json.writes[Item]

  def apply(idx: Option[Int], groupIdx: Option[Int], title: Option[String], status: Option[Int],
            memo: Option[String], tag: Option[String], `type`: Option[Int], repeatType: Option[Int],
            itemDatetime: Option[LocalDateTime], createAt: Option[LocalDateTime]): Item =
    new Item(idx, groupIdx, title, status.fold(Option(1))(i => Some(i)), memo, tag, `type`.fold(Option(1))(i => Some(i)),
      repeatType.fold(Option(0))(i => Some(i)), itemDatetime, createAt)
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
