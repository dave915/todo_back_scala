package dao

import java.sql.Date

import javax.inject.{Inject, Singleton}
import models.Item
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
class Items(tag: Tag) extends Table[Item](tag, "item") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def groupIdx = column[Option[Int]]("groupIdx")
  def title = column[Option[String]]("title")
  def status = column[Option[Int]]("status")
  def memo = column[Option[String]]("memo")
  def itemTag = column[Option[String]]("tag")
  def `type` = column[Option[Int]]("type")
  def startDatetime = column[Option[Date]]("startDatetime")
  def endDatetime = column[Option[Date]]("endDatetime")

  override def * = (idx, groupIdx, title, status, memo, itemTag, `type`, startDatetime, endDatetime) <>
    ((Item.apply _).tupled, Item.unapply)
}

@Singleton
class ItemDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                        implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val items = TableQuery[Items]

  def save(item: Item) = {
    db.run(items insertOrUpdate item)
  }

  def delete(idx: Int) = {
    db.run(items.filter(_.idx === idx).delete)
  }

  def getItemListByGroupIdx(groupIdx: Int) = {
    db.run(items.filter(_.groupIdx === groupIdx).result)
  }
}
