package controllers

import java.io.File
import javax.inject.Inject

import com.google.common.io.Files
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData.FilePart
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Muhsin Ali on 01/10/2016.
  */

/**
  * PlaceController - acts as a DAO to Place objects stored in the database.
  */
class PlaceController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {

  def create(placeData: PlaceData, picture: FilePart[TemporaryFile]): Future[WriteResult] = {
    for {
      places <- placesFuture
      writeResult <- places.insert(Place(PlaceController.generateID, placeData.name, placeData.country, placeData.description,
        Files.toByteArray(picture.ref.file)))
    } yield {
      writeResult
    }
  }

  def create(id: Int, name: String, country: String, description: String, picture: File): Future[WriteResult] = {
    for {
      places <- placesFuture
      writeResult <- places.insert(Place(id, name, country, description, Files.toByteArray(picture)))
    } yield {
      writeResult
    }
  }

  def drop() = placesFuture.map(_.drop(failIfNotFound = true))


  def findById(id: Int): Future[Option[Place]] = findOne(Json.obj("id" -> id))

  def findMany(jsObject: JsObject): Future[List[Place]] = placesFuture.flatMap(_.find(jsObject)
    .cursor[Place](ReadPreference.primary).collect[List]())

  def findOne(jsObject: JsObject): Future[Option[Place]] = placesFuture.flatMap(_.find(jsObject).one[Place](ReadPreference.primary))

  def getAllPlaces: Future[List[Place]] = findMany(Json.obj())

  def placesFuture: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))

  def remove(id: Int): Future[Boolean] = {
    for {
      placeToDelete <- findById(id)
      places <- placesFuture
      writeResult <- places.remove[Place](placeToDelete.get, firstMatchOnly = true)
    } yield {
      placeToDelete.isDefined && writeResult.ok
    }
  }

  def update(placeData: PlaceData, pictureOpt: Option[FilePart[TemporaryFile]]): Future[UpdateWriteResult] = {
    val id = placeData.id.get   // IntelliJ complains of a type mismatch at compile-time if I place it in the for-comprehension below
    for {
      places <- placesFuture
      placeOpt <- findById(id)
      picture = if(pictureOpt.get.filename != "") Files.toByteArray(pictureOpt.get.ref.file) else placeOpt.get.picture
      updateWriteResult <- places.update(Json.obj("id" -> id), Place(id, placeData.name, placeData.country, placeData.description, picture))
    } yield {
      updateWriteResult
    }
  }
}



object PlaceController {
  /* TODO could use something like java.util.UUID.randomUUID.toString for a unique ID, but then the URL will include it
      in various route definitions, making it unreadable.
      So, not sure what would be the best way to make URLs human readable whilst guaranteeing that they bind to unique Place objects.
   */
  private var placeID: Int = 0
  def generateID: Int = {
    placeID += 1
    placeID
  }

  val createPlaceForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "country" -> nonEmptyText,
      "description" -> nonEmptyText
    )(PlaceData.apply)(PlaceData.unapply)
  )
}