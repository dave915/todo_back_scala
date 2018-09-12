package service

import dao.ItemDao
import javax.inject.{Inject, Singleton}
import models.Item

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
@Singleton
class ItemService @Inject()(private val itemDao: ItemDao,
                            implicit val ec: ExecutionContext) {
  def save(item: Item) = {
    itemDao.save(item)
  }

  def delete(idx: Int) = {
    itemDao.delete(idx)
  }
}
