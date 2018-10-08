package service

import javax.inject.{Inject, Singleton}
import models.{Group, GroupDataAccess, GroupInfo, JoinGroupDataAccess}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupService @Inject()(private val groupDataAccess: GroupDataAccess,
                             private val joinGroupDataAccess: JoinGroupDataAccess,
                             implicit val ec: ExecutionContext) {
  def getJoinGroupByUserIdx(idx: Int): Future[Seq[GroupInfo]] = {
    groupDataAccess.getJoinGroupByUserIdx(idx) map { group =>
      group.map(groupInfo => {
        GroupInfo(groupInfo._1.idx.get, groupInfo._1.name.get, groupInfo._1.isDefaultGroup.get, groupInfo._2)
      })
    }
  }

  def addGroup(group: Group, idx: Int): Unit = {
    groupDataAccess.insert(group.name.get, isDefaultGroup = false).onComplete { group =>
      joinGroupDataAccess.insert(group.get.idx.get, idx, 1)
    }
  }

  def getJoinUsers(groupIdx: Int) = {
    joinGroupDataAccess.getJoinUsers(groupIdx)
  }
}
