package dao

import javax.inject.{Inject, Singleton}
import models.JoinGroup
import slick.lifted.Tag
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
class JoinGroups(tag: Tag) extends Table[JoinGroup](tag, "join_group") {
  def groupIdx = column[Int]("groupIdx")
  def userIdx= column[Int]("userIdx")
  def `type` = column[Int]("type")


  override def * = (groupIdx, userIdx, `type`) <> ((JoinGroup.apply _).tupled, JoinGroup.unapply)
}

@Singleton
class JoinGroupDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                             implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val joinGroups = TableQuery[JoinGroups]

  def insert(groupIdx: Int, userIdx: Int, `type`: Int): Unit = {
    db.run(joinGroups += JoinGroup(groupIdx, userIdx, `type`))
  }
}
