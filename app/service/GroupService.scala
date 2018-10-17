package service

import javax.inject.{Inject, Singleton}
import models._

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
      joinGroupDataAccess.save(JoinGroup(group.get.idx.get, idx, 1))
    }
  }

  def getJoinUsers(groupIdx: Int) = {
    joinGroupDataAccess.getJoinUsers(groupIdx).map { list =>
      list.unzip._1
    }
  }

  def inviteUser(groupIdx: Int, userIdx: Int, requestUserId: Int) = {

    for {
      // request 유저가 그룹 오너의 요청인지 체크
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, requestUserId)
    } yield {
      if (currentUser.`type` == 1)
        for {
          // 이미 참여중인 유저인지 체크
          inviteUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
        } yield {
          if(inviteUser eq null)
            joinGroupDataAccess.save(JoinGroup(groupIdx, userIdx, 3))
          else
            throw new RuntimeException("이미 참여 중이거나 참여 진행중인 유저입니다.")
        }
      else
        throw new RuntimeException("그룹 소유자가 아닙니다.")
    }
  }

  def joinGroup(groupIdx: Int, userIdx: Int) = {
    joinGroupDataAccess.save(JoinGroup(groupIdx, userIdx, 2))
  }

  def changeGroupOwner(groupIdx: Int, userIdx: Int, ownerIdx: Int) = {
    joinGroupDataAccess.save(JoinGroup(groupIdx, userIdx, 1))
    joinGroupDataAccess.save(JoinGroup(groupIdx, ownerIdx, 2))
  }

  def leaveGroup(groupIdx: Int, userIdx: Int): Unit = {
    val newOwner: Future[User] = for {
      currentUser <- joinGroupDataAccess.checkJoinUser(groupIdx, userIdx)
      if currentUser.`type` == 1
      users <- joinGroupDataAccess.getJoinUsers(groupIdx)
    } yield users.filter(!_._1.idx.equals(Option(userIdx))).minBy(_._2.toString)._1

    newOwner.map(user => joinGroupDataAccess.save(JoinGroup(groupIdx, user.idx.get, 1)))
    joinGroupDataAccess.delete(groupIdx, userIdx)
  }
}
