package controllers

import java.nio.file.{Files, Paths}
import javax.inject.Inject

import models.Place
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Muhsin Ali on 01/10/2016.
  */

//
class PlaceController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {

  def placesFuture: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))

  def create(id: Int, name: String, country: String, description: String, pictureURL: String) = Action.async {
    for {
      places <- placesFuture
      writeResult <- places.insert(Place(id, name, country, description, Files.readAllBytes(Paths.get(pictureURL))))
    } yield
      Ok(s"writeResult: $writeResult\n")
  }

  def retrieveAllPlaces(): Future[List[Place]] = {
    // let's do our query
    val placesList: Future[List[Place]] = placesFuture.flatMap {
      // find all Places with name `name`
      _.find(Json.obj()).
        // perform the query and get a cursor of JsObject
        cursor[Place](ReadPreference.primary).
        // Collect the results as a list
        collect[List]()
    }
    placesList
  }


  def findBy(jsObject: JsObject) = Action.async {
    val placesList: Future[List[Place]] = placesFuture.flatMap{
      _.find(jsObject).
        cursor[Place](ReadPreference.primary).
        collect[List]()
    }
    placesList.map { places => Ok(Json.toJson(places))}
  }

  def findById(id: Int) = findBy(Json.obj("id" -> id))


  // TODO make the query such that it only finds 1 Place. Then return the picture on that.
  def retrievePictureOfPlace(id: Int) = Action.async {
    val placesList: Future[List[Place]] = placesFuture.flatMap {
      _.find(Json.obj("id" -> id)).cursor[Place](ReadPreference.primary).collect[List]()
    }
    placesList.map { places => Ok(places.head.picture)}
  }
}
