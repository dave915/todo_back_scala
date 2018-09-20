package service

import javax.inject.{Inject, Singleton}
import models.{Item, ItemDataAccess}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class ItemService @Inject()(private val itemDataAccess: ItemDataAccess,
                            implicit val ec: ExecutionContext) {
  def getItemListByGroupIdx(groupIdx: Int): Future[Seq[Item]] = {
    itemDataAccess.getItemListByGroupIdx(groupIdx)
  }

  def save(item: Item): Future[Int] = {
    itemDataAccess.save(item)
  }

  def delete(idx: Int): Future[Int] = {
    itemDataAccess.delete(idx)
  }
}
