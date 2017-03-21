package dao

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
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection
import sun.misc.BASE64Encoder

import scala.concurrent.{ExecutionContext, Future}


/**
  * PlaceDAO - acts as a DAO to instances of the Place class that are stored in the database.
  */
class PlaceDAO @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {

  private val base64Encoder = new BASE64Encoder()

  // Must be a 'def' and not a 'val' to prevent problems in development in Play with hot-reloading
  private def placesCollection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))



  // Used to create Place objects from a form submitted by the user
  def create(placeData: PlaceData, pictureOpt: Option[FilePart[TemporaryFile]], key: String): Future[WriteResult] = {
    // IntelliJ complains of a type mismatch at compile-time if I place it in the for-comprehension below
    val picture = pictureOpt.get
    for {
      places <- placesCollection
      writeResult <- places.insert(Place(PlaceDAO.generateID, placeData.name, placeData.country, placeData.description,
        base64Encoder.encode(Files.toByteArray(picture.ref.file)), key))
    } yield writeResult
  }

  // Used to create instances of the Place class from JSON files at application startup
  def create(id: Int, name: String, country: String, description: String, picture: File, key: String): Future[WriteResult] = {
    for {
      places <- placesCollection
      writeResult <- places.insert(Place(id, name, country, description, base64Encoder.encode(Files.toByteArray(picture)), key))
    } yield writeResult
  }

  def drop() = placesCollection.map(_.drop(failIfNotFound = true))

  def findById(id: Int): Future[Option[Place]] = findOne(Json.obj("id" -> id))

  def findMany(jsObject: JsObject): Future[List[Place]] = placesCollection.flatMap(_.find(jsObject)
    .cursor[Place](ReadPreference.primaryPreferred).collect[List](Int.MaxValue, Cursor.FailOnError[List[Place]]()))

  // Might be able to use the OptionT monad transformer from Cats here - might remove the need to unwrap Options in
  // some of the methods in this class
  def findOne(jsObject: JsObject): Future[Option[Place]] = placesCollection.flatMap(_.find(jsObject).one[Place](ReadPreference.primary))

  def getAllPlaces: Future[List[Place]] = findMany(Json.obj())

  def remove(id: Int): Future[Boolean] = {
    for {
      placeToDelete <- findById(id)
      if placeToDelete.isDefined
      places <- placesCollection
      writeResult <- places.remove[Place](placeToDelete.get, firstMatchOnly = true)
    } yield writeResult.ok
  }

  def update(placeData: PlaceData, pictureOpt: Option[FilePart[TemporaryFile]], key: String): Future[UpdateWriteResult] = {
    val id = placeData.id.get   // IntelliJ complains of a type mismatch at compile-time if I place it in the for-comprehension below
    for {
      places <- placesCollection
      placeOpt <- findById(id)
      picture = if(pictureOpt.get.filename != "") base64Encoder.encode(Files.toByteArray(pictureOpt.get.ref.file)) else placeOpt.get.picture
      updateWriteResult <- places.update(Json.obj("id" -> id), Place(id, placeData.name, placeData.country, placeData.description, picture, key))
    } yield updateWriteResult
  }
}




object PlaceDAO {
  /* STOPSHIP
   NOTE:
   The method used here to generate IDs is not the best. Could use a GUID (e.g. one generated using the
   BSONObjectID.generate method) but then this would make the URL harder to read. So for the time being generate a simple
   ID (would change this method if it were to go into production).

   As a side note, including the ID in the URL is bad practice as it also exposes the internals of the application to the
   user (which, in this case, would be the ID of a Place stored in the database). This is a security vulnerability.

   As a result, would prefer to use a method that generates a readable, self descriptive URL that's unique to each
   Place object, for example:
   https://www.example.com/this-is-a-self-descriptive-url
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