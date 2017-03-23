package daos

import java.io.File
import java.util.UUID
import javax.inject.Inject

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData.FilePart
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
  * PlaceDAO - acts as a DAO to instances of the Place class that are stored in the database.
  */
class PlaceDAO @Inject()(val reactiveMongoApi: ReactiveMongoApi, config: Configuration)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {
  // Must be a 'def' and not a 'val' to prevent problems in development in Play with hot-reloading
  private def placesCollection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("places"))

  private val s3DAO = new S3DAO(config.underlying.getString("aws-s3-bucket-name"))


  // Used to create Place objects from a form submitted by the user
  def create(placeData: PlaceData, pictureOpt: Option[FilePart[TemporaryFile]]): Future[WriteResult] = {
    val uuid = UUID.randomUUID()
    s3DAO.uploadFile(placeData.name, uuid, pictureOpt.get.ref.file)

    for {
      places <- placesCollection
      writeResult <- places.insert(Place(PlaceDAO.generateID, placeData.name, placeData.country, placeData.description,
        s3DAO.urlPrefix, uuid.toString))
    } yield writeResult
  }

  // Used to create instances of the Place class from JSON files at application startup
  def create(id: Int, name: String, country: String, description: String, picture: File): Future[WriteResult] = {
    val uuid = UUID.randomUUID()
    s3DAO.uploadFile(name, uuid, picture)
    for {
      places <- placesCollection
      writeResult <- places.insert(Place(id, name, country, description, s3DAO.urlPrefix, uuid.toString))
    } yield writeResult
  }

  def drop(): Future[Boolean] = {
    s3DAO.emptyBucket()
    placesCollection.flatMap(_.drop(failIfNotFound = true))
  }

  def findById(id: Int): Future[Option[Place]] = findOne(Json.obj("id" -> id))

  def findMany(jsObject: JsObject): Future[List[Place]] = placesCollection.flatMap(_.find(jsObject)
    .cursor[Place](ReadPreference.primaryPreferred).collect[List](Int.MaxValue, Cursor.FailOnError[List[Place]]()))

  // Might be able to use the OptionT monad transformer from Cats here - might remove the need to unwrap Options in
  // some of the methods in this class
  def findOne(jsObject: JsObject): Future[Option[Place]] = placesCollection.flatMap(_.find(jsObject).one[Place](ReadPreference.primary))

  def getAllPlaces: Future[List[Place]] = findMany(Json.obj())

  def remove(id: Int): Future[Boolean] = {
    findById(id).onComplete{
      case Success(placeToDelete) => s3DAO.deleteFile(placeToDelete.get)
      case Failure(_) => Logger.error("Error in PlaceDAO.remove() - Place could not be located in database")
    }

    for {
      placeToDelete <- findById(id)
      places <- placesCollection
      writeResult <- places.remove[Place](placeToDelete.get, firstMatchOnly = true)
    } yield writeResult.ok
  }


  def update(placeData: PlaceData, pictureOpt: Option[FilePart[TemporaryFile]]): Future[UpdateWriteResult] = {
    val id = placeData.id.get
    val didUserUploadNewImage = pictureOpt.get.filename != ""

    if(didUserUploadNewImage){
      findById(id).onComplete{
        case Success(placeToDelete) => s3DAO.deleteFile(placeToDelete.get)
        case Failure(_) => Logger.error("Error in PlaceDAO.update() - Place could not be located in database")
      }
    }

    val uuid = UUID.randomUUID()
    for {
      places <- placesCollection
      placeOpt <- findById(id)
      updateWriteResult <- places.update(Json.obj("id" -> id),
        new Place(placeData, s3DAO.urlPrefix, uuid.toString))
    } yield updateWriteResult
  }
}




object PlaceDAO {
  /*
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