package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.twirl.api.Html
import play.modules.reactivemongo.json._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {
  implicit val formatter = Json.format[Place]

  val placeController = new PlaceController(reactiveMongoApi)

  // TODO this is blocking, find out how to make it non-blocking
  def index = Action { implicit request =>
    val jsonInfo = Json.prettyPrint(Json.toJson(Await.result(placeController.retrieveAllPlaces(), 1 seconds)))
    Ok(views.html.main("")(Html(""))(Html(jsonInfo)))
  }

  // TODO this is blocking, find out how to make it non-blocking
  def showList() = Action { implicit request =>
    Ok(views.html.list(Await.result(placeController.retrieveAllPlaces(), 1 seconds)))
  }

  def showGrid() = TODO

  // TODO this is blocking, find out how to make it non-blocking
  def showPlace(id: Int) = Action { implicit request =>
    val placeOpt: Option[Place] = Await.result(placeController.placesFuture.flatMap(_.find(Json.obj("id" -> id)).one[Place]), 1 seconds)
    if(placeOpt.isDefined){
      Ok(views.html.showPlace(placeOpt.get))
    } else {
      BadRequest("Uh oh")
    }
  }

  def displayPlaceForm() = TODO

}

