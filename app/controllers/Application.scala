package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
import models.dal.LibraryRepository
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.twirl.api.Html

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {
  implicit val formatter = Json.format[Place]

  val placeController = new PlaceController(reactiveMongoApi)

  def index = Action { implicit request =>
    val jsonInfo = Json.prettyPrint(Json.toJson(LibraryRepository.placesList))
    Ok(views.html.main("")(Html(jsonInfo)))
  }

  // TODO this is blocking, find out how to make it non-blocking
  def showList =
    Action { implicit request =>
    Ok(views.html.list(Await.result(placeController.retrieveAllPlaces(), 1 seconds)))
  }

  def showGrid = TODO
}

