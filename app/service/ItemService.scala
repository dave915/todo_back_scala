package service

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import models._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class ItemService @Inject()(private val itemDataAccess: ItemDataAccess,
                            private val groupDataAccess: GroupDataAccess,
                            implicit val ec: ExecutionContext) {
  def getItemList(itemSearchOption: ItemSearchOption, user: User): Future[Seq[Item]] = {
    var idxList: List[Int] = null
    itemSearchOption.groupIdx.fold {
      val getJoinGroupIdx = groupDataAccess.getJoinGroupByUserIdx(user.idx.get).map(_.map(_._1.idx.get))
        .map(list => idxList = list.toList)
      Await.result(getJoinGroupIdx, Duration.Inf)
    } { groupIdx => idxList = List(groupIdx)}

    itemDataAccess.getItemListBySearchOption(itemSearchOption.copy(groupIdxList = Option(idxList)))
  }

  def save(item: Item): Future[Int] = {
    val saveItem = if(item.idx.isEmpty) item.copy(createAt = Some(LocalDateTime.now())) else item
    itemDataAccess.save(saveItem)
  }

  def addRepeatItem(item: Item): Any = {
    val nextItemDateTime = item.repeatType.get match {
      case 1 => item.itemDatetime.get.plusDays(1)
      case 2 => item.itemDatetime.get.plusWeeks(1)
      case 3 => item.itemDatetime.get.plusMonths(1)
      case 4 => item.itemDatetime.get.plusYears(1)
      case _ => null
    }

    if(nextItemDateTime != null)
      itemDataAccess.save(item.copy(idx = None, itemDatetime = Some(nextItemDateTime), createAt = Some(LocalDateTime.now())))
  }

  def delete(idx: Int): Future[Int] = {
    itemDataAccess.delete(idx)
  }
}
