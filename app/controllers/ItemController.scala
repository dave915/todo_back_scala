package controllers

import javax.inject.Inject
import models.{Item, ItemSearchOption}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ItemService

import scala.concurrent.ExecutionContext

/**
  * @author dave.th
  * @date 2018. 9. 12.
  */
class ItemController @Inject()(auth: SecuredAuthenticator,
                               itemService: ItemService,
                               cc: ControllerComponents,
                               implicit val ec: ExecutionContext) extends AbstractController(cc) {
  def getList = auth.JWTAuthentication.async { implicit request =>
    val itemSearchOption = ItemSearchOption.convert(request.queryString.map {case (k,v) => k -> v.mkString})
    itemService.getItemList(itemSearchOption, request.user) map { item =>
      Ok(Json.toJson(item))
    }
  }

  def save = auth.JWTAuthentication.async { implicit request =>
    val item = request.body.asJson.get.as[Item]

    itemService.save(item)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def addRepeatItem = auth.JWTAuthentication.async { implicit request =>
    val item = request.body.asJson.get.as[Item]

    itemService.addRepeatItem(item)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }

  def delete(idx: Int) = auth.JWTAuthentication.async {
    itemService.delete(idx)
      .map( _ => Ok(Json.toJson(Map("result" -> "success"))))
      .recover {
        case e: Exception => BadRequest(Json.toJson(Map("result" -> "fail")))
      }
  }
}
