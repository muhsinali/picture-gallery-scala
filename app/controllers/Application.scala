package controllers

import java.io.File
import javax.inject.Inject

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import models.{Place, PlaceData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.ApplicationLifecycle
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONObjectIDFormat   // implicit format for BSONObjectID

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}


/**
  * Created by Muhsin Ali on 29/09/2016.
  */

/**
  * This web application stores places of interest in a database and displays them either using a list or a grid layout.
  * The user can add, edit or delete places from the database.
  *
  * Application is the entry point of this web application, and handles all HTTP requests for this web application.
  */
class Application @Inject()(val messagesApi: MessagesApi, val reactiveMongoApi: ReactiveMongoApi, applicationLifecycle: ApplicationLifecycle)
                           (implicit ec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  implicit val formatter = Json.format[Place]
  val placeController = new PlaceController(reactiveMongoApi)

  onStartup()
  applicationLifecycle.addStopHook {() => onShutdown()}


  /**
    * Populates database with Places at application startup
    */
  def onStartup() = {
    // Get a list of all the files in a directory
    def getListOfFiles(dirPath: String) = {
      val dir = new File(dirPath)
      if(dir.exists() && dir.isDirectory){
        dir.listFiles.filter(f => f.isFile && f.toString.endsWith(".json")).toList
      } else {
        List[File]()
      }
    }

    def getJsonProperty(jsValue: JsValue, field: String) = (jsValue \ field).get.toString().replace("\"", "")

    val jsonFiles = getListOfFiles("./public/jsonFiles")
    for(f <- jsonFiles) {
      // TODO might be handy to use parsedJson.as[Place] here - but Place.picture is of type Array[Byte]
      val source = Source.fromFile(f)
      val parsedJson: JsValue = Json.parse(source.mkString)
      val id = BSONObjectID.parse(getJsonProperty(parsedJson, "_id"))
      val name = getJsonProperty(parsedJson, "name")
      val country = getJsonProperty(parsedJson, "country")
      val description = getJsonProperty(parsedJson, "description")
      val picture = new File(getJsonProperty(parsedJson, "picture"))
      source.close()

      id match {
        case Success(v) => placeController.create(v, name, country, description, picture)
        case Failure(e) => throw e
      }
    }
  }

  /**
    * Clears database at application shutdown
    */
  def onShutdown() = {
    placeController.drop()
  }



  // TODO get the flash scope to work
  def deletePlace(id: BSONObjectID) = Action.async { implicit request =>
    placeController.remove(id).map(wasPlaceRemoved => {
      if(wasPlaceRemoved){
        Redirect(routes.Application.showGridView()).flashing("success" -> s"Deleted place with ID $id")
      } else {
        Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not delete place with ID $id")
      }
    })
  }

  // TODO find out how to fill the picture field with the picture's filename. It is not part of the PlaceData class.
  def editPlace(id: BSONObjectID) = Action.async { implicit request =>
    placeController.findById(id).map(placeOpt => {
      if(placeOpt.isDefined){
        val placeFound = placeOpt.get
        val placeData = PlaceData(placeFound.name, placeFound.country, placeFound.description)
        Ok(views.html.placeForm(PlaceController.createPlaceForm.fill(placeData)))
      } else {
        Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not find place with $id")
      }
    })
  }

  def getPictureOfPlace(id: BSONObjectID) = Action.async {
    placeController.findById(id).map(placeOpt => {
      if(placeOpt.isDefined){
        Ok(placeOpt.get.picture)
      } else {
        BadRequest(s"Could not find picture for Place with ID $id")
      }
    })
  }

  def showGridView() = Action.async { implicit request =>
    placeController.getAllPlaces.map(placesList => {
      val numColumns = 3
      val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
      Ok(views.html.grid(placesList, numRows, numColumns))
    })
  }

  def showListView = Action.async { implicit request =>
    placeController.getAllPlaces.map(placesList => Ok(views.html.list(placesList)))
  }

  def showPlace(id: BSONObjectID) = Action.async { implicit request =>
    placeController.findById(id).map(placeOpt => {
      if (placeOpt.isDefined) {
        Ok (views.html.showPlace(placeOpt.get))
      } else {
        Redirect(routes.Application.showGridView()).flashing("error" -> s"Cannot find place with id $id")
      }
    })
  }

  def showPlaceForm = Action { implicit request =>
    Ok(views.html.placeForm(PlaceController.createPlaceForm))
  }

  // TODO find out how to make the picture a required field
  /**
    * Handles the form post, inserting the new Place into the database
    */
  def uploadPlace() = Action.async(parse.multipartFormData) { implicit request =>
    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        Future(BadRequest(views.html.placeForm(formWithErrors)))
      },
      placeData => {
        val writeResultFuture = placeController.create(placeData, request.body.file("picture").get)
        writeResultFuture.map(writeResult => {
          if(writeResult.ok){
            Redirect(routes.Application.showGridView()).flashing("success" -> "Successfully added Place")
          } else {
            Redirect(routes.Application.showGridView()).flashing("error" -> "Could not add Place to database")
          }
        })
      }
    )
  }
}

