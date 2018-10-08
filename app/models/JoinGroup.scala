package models

import javax.inject.{Inject, Singleton}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, OWrites, Reads}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class JoinGroupDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val joinGroups = TableQuery[JoinGroups]
  val users = TableQuery[Users]

  def insert(groupIdx: Int, userIdx: Int, `type`: Int): Unit = {
    db.run(joinGroups += JoinGroup(groupIdx, userIdx, `type`))
  }

  def getJoinUsers(groupIdx: Int) = {
    val query = (joinGroups.filter(_.groupIdx === groupIdx) join users on (_.userIdx === _.idx))
      .map { case (j, u) => u }

    db.run(query.result)
  }
}

case class JoinGroup(groupIdx: Int, userIdx: Int, `type`: Int)

object JoinGroup {
  implicit val reads: Reads[JoinGroup] = Json.reads[JoinGroup]
  implicit val writes: OWrites[JoinGroup] = Json.writes[JoinGroup]
}

class JoinGroups(tag: Tag) extends Table[JoinGroup](tag, "join_group") {
  def groupIdx = column[Int]("groupIdx")
  def userIdx= column[Int]("userIdx")
  def `type` = column[Int]("type")

  override def * = (groupIdx, userIdx, `type`) <> ((JoinGroup.apply _).tupled, JoinGroup.unapply)
}
