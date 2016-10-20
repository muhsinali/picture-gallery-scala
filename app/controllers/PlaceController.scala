package controllers

import com.google.common.io.Files
import javax.inject.Inject

import models._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Muhsin Ali on 01/10/2016.
  */

class PlaceController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {

  def placesFuture: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))

//  // TODO improve this

  def create(placeData: PlaceData, picture: FilePart[TemporaryFile]): Future[WriteResult] = {
    for {
      places <- placesFuture
      numPlaces <- places.count()
      writeResult <- places.insert(Place(numPlaces, placeData.name, placeData.country, placeData.description, Files.toByteArray(picture.ref.file)))
    } yield {
      writeResult
    }
  }

  def retrieveAllPlaces(): Future[List[Place]] = {
    val placesList: Future[List[Place]] = placesFuture.flatMap {
      _.find(Json.obj()).
        cursor[Place](ReadPreference.primary).
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

object PlaceController {
  val createPlaceForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "country" -> nonEmptyText,
      "description" -> nonEmptyText
    )(PlaceData.apply)(PlaceData.unapply)
  )
}