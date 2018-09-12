package dao

import javax.inject.{Inject, Singleton}
import models.{Group, GroupInfo}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
class Groups(tag: Tag) extends Table[Group](tag, "group") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def name = column[Option[String]]("name")
  def isDefaultGroup = column[Option[Boolean]]("isDefaultGroup")

  override def * = (idx, name, isDefaultGroup) <> ((Group.apply _).tupled, Group.unapply)
}

@Singleton
class GroupDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                          implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val groups = TableQuery[Groups]
  val joinGroups = TableQuery[JoinGroups]

  def insert(groupName: String, isDefaultGroup: Boolean) = {
    val query = (groups returning groups.map(_.idx) into ((group, idx) => group.copy(idx = idx))) +=
      Group(None, Some(groupName), Some(isDefaultGroup))
    db.run(query)
  }

  def delete(idx: Int): Unit = {
    db.run(groups.filter(_.idx === idx).delete)
  }

  def getJoinGroupByUserIdx(userIdx: Int) = {
    val query = (joinGroups.filter(_.userIdx === userIdx) join groups on (_.groupIdx === _.idx))
      .map{ case (j, g) => (g, j.`type`)}

    db.run(query.result)
  }
}
