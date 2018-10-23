package models

import java.time.LocalDateTime

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
class GroupDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                         implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val groups = TableQuery[Groups]
  val joinGroups = TableQuery[JoinGroups]

  def insert(groupName: String, isDefaultGroup: Boolean): Future[Group] = {
    val query = (groups returning groups.map(_.idx) into ((group, idx) => group.copy(idx = idx))) +=
      Group(None, Some(groupName), Some(isDefaultGroup), Some(LocalDateTime.now()))
    db.run(query)
  }

  def update(group: Group) = {
    val q = for { g <- groups if g.idx === group.idx.get } yield g.name
    db.run(q.update(group.name))
  }

  def delete(idx: Int): Unit = {
    db.run(groups.filter(_.idx === idx).delete)
  }

  def getJoinGroupByUserIdx(userIdx: Int): Future[Seq[(Group, Int)]] = {
    val query = (joinGroups.filter(_.userIdx === userIdx) join groups on (_.groupIdx === _.idx))
      .map{ case (j, g) => (g, j.`type`)}

    db.run(query.result)
  }
}

case class Group(idx: Option[Int], name: Option[String], isDefaultGroup: Option[Boolean], createAt: Option[LocalDateTime])

object Group {
  implicit val reads: Reads[Group] = Json.reads[Group]
  implicit val writes: OWrites[Group] = Json.writes[Group]
}

class Groups(tag: Tag) extends Table[Group](tag, "group") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def name = column[Option[String]]("name")
  def isDefaultGroup = column[Option[Boolean]]("isDefaultGroup")
  def createAt = column[Option[LocalDateTime]]("createAt")

  override def * = (idx, name, isDefaultGroup, createAt) <> ((Group.apply _).tupled, Group.unapply)
}
