package service

import dao.{GroupDao, JoinGroupDao}
import javax.inject.{Inject, Singleton}
import models.{Group, GroupInfo}

import scala.concurrent.ExecutionContext

@Singleton
class GroupService @Inject()(private val groupDao: GroupDao,
                             private val joinGroupDao: JoinGroupDao,
                             implicit val ec: ExecutionContext) {

  def getJoinGroupByUserIdx(idx: Int) = {
    groupDao.getJoinGroupByUserIdx(idx) map { group =>
      group.map(groupInfo => {
        GroupInfo(groupInfo._1.idx.get, groupInfo._1.name.get, groupInfo._1.isDefaultGroup.get, groupInfo._2)
      })
    }
  }

  def addGroup(group: Group, idx: Int): Unit = {
    groupDao.insert(group.name.get, false).onComplete { group =>
      joinGroupDao.insert(group.get.idx.get, idx, 1)
    }
  }
}
