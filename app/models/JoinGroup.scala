package models

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import models.conversions.LocalDateTimeTableConversions._
import play.api.libs.functional.syntax._

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class JoinGroupDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val joinGroups = TableQuery[JoinGroups]
  val users = TableQuery[Users]

  def save(joinGroup: JoinGroup) = {
    db.run(joinGroups insertOrUpdate joinGroup)
  }

  def delete(groupIdx: Int, userIdx: Int) = {
    db.run(joinGroups.filter(joinGroup =>
      joinGroup.groupIdx === groupIdx && joinGroup.userIdx === userIdx
    ).delete)
  }

  def getJoinUsers(groupIdx: Int) = {
    val query = (joinGroups.filter(_.groupIdx === groupIdx) join users on (_.userIdx === _.idx))
      .map { case (j, u) => (u, j)}

    db.run(query.result)
  }

  def checkJoinUser(groupIdx: Int, userIdx: Int) = {
    val query = joinGroups.filter(joinGroups =>
      joinGroups.groupIdx === groupIdx &&
        joinGroups.userIdx === userIdx)

    db.run(query.result.headOption)
  }
}

case class JoinGroup(groupIdx: Int, userIdx: Int, `type`: Int, createAt: LocalDateTime = LocalDateTime.now())

object JoinGroup {
  implicit val reads: Reads[JoinGroup] = (
    (JsPath \ "groupIdx").read[Int] and
      (JsPath \ "userIdx").read[Int] and
      (JsPath \ "type").read[Int]
  )((groupIdx: Int, userIdx: Int, `type`: Int) => JoinGroup(groupIdx, userIdx, `type`))

  implicit val writes: OWrites[JoinGroup] = (o: JoinGroup) => Json.obj(
    "groupIdx" -> o.groupIdx,
    "userIdx" -> o.userIdx,
    "type" -> o.`type`,
  )
}

class JoinGroups(tag: Tag) extends Table[JoinGroup](tag, "join_group") {
  def groupIdx = column[Int]("groupIdx")
  def userIdx= column[Int]("userIdx")
  def `type` = column[Int]("type")
  def createAt= column[LocalDateTime]("createAt")

  override def * = (groupIdx, userIdx, `type`, createAt) <> ((JoinGroup.apply _).tupled, JoinGroup.unapply)
}
