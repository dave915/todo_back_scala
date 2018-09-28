package service

import javax.inject.{Inject, Singleton}
import models.{Item, ItemDataAccess, ItemSearchOption}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class ItemService @Inject()(private val itemDataAccess: ItemDataAccess,
                            implicit val ec: ExecutionContext) {
  def getItemList(itemSearchOption: ItemSearchOption): Future[Seq[Item]] = {
    itemDataAccess.getItemListBySearchOption(itemSearchOption)
  }

  def save(item: Item): Future[Int] = {
    itemDataAccess.save(item)
  }

  def delete(idx: Int): Future[Int] = {
    itemDataAccess.delete(idx)
  }
}
