package dao

import javax.inject.{Inject, Singleton}
import models.Group
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
class Groups(tag: Tag) extends Table[Group](tag, "group") {
  def idx = column[Option[Int]]("idx", O.PrimaryKey, O.AutoInc)
  def name = column[Option[String]]("name")

  override def * = (idx, name) <> ((Group.apply _).tupled, Group.unapply)
}

@Singleton
class GroupDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                          implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  val groups = TableQuery[Groups]

  def insert(groupName: String) = {
    val action = (groups returning groups.map(_.idx) into ((group, idx) => group.copy(idx = idx))) +=
      Group(None, Some(groupName))
    db.run(action)
  }

  def delete(idx: Int): Unit = {
    groups.filter(_.idx === idx).delete
  }
}
