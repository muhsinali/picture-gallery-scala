package controllers

import java.io.File

import com.google.common.io.Files
import javax.inject.Inject

import models._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Controller
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Muhsin Ali on 01/10/2016.
  */

/**
  * PlaceController - acts as a DAO to Place objects stored in the MongoDB database.
  */
class PlaceController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {

  def create(placeData: PlaceData, picture: FilePart[TemporaryFile]): Future[WriteResult] = {
    for {
      places <- placesFuture
      writeResult <- places.insert(Place(BSONObjectID.generate(), placeData.name, placeData.country, placeData.description,
        Files.toByteArray(picture.ref.file)))
    } yield {
      writeResult
    }
  }

  def create(id: BSONObjectID, name: String, country: String, description: String, picture: File): Future[WriteResult] = {
    for {
      places <- placesFuture
      writeResult <- places.insert(Place(id, name, country, description, Files.toByteArray(picture)))
    } yield {
      writeResult
    }
  }

  def drop() = {
    placesFuture.map(_.drop(failIfNotFound = true))
  }


  def findById(id: BSONObjectID): Future[Option[Place]] = findOne(Json.obj("id" -> id))

  def findMany(jsObject: JsObject): Future[List[Place]] = {
    placesFuture.flatMap{
      _.find(jsObject).
        cursor[Place](ReadPreference.primary).
        collect[List]()
    }
  }

  def findOne(jsObject: JsObject): Future[Option[Place]] = placesFuture.flatMap{_.find(jsObject).one[Place](ReadPreference.primary)}

  def getAllPlaces: Future[List[Place]] = findMany(Json.obj())

  def placesFuture: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))

  def remove(id: BSONObjectID): Future[Boolean] = {
    for {
      placeToDelete <- findById(id)
      places <- placesFuture
      writeResult <- places.remove[Place](placeToDelete.get, firstMatchOnly = true)
    } yield {
      placeToDelete.isDefined && writeResult.ok
    }
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