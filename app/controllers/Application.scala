package controllers

import java.io.File
import javax.inject.Inject

import models.{Place, PlaceData}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source


/**
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
    * Populates database with Places at application startup.
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

    // TODO might be handy to use parsedJson.as[Place] here - but Place.picture is of type Array[Byte]
    val jsonFiles = getListOfFiles("./public/jsonFiles")
    for(f <- jsonFiles) {
      val source = Source.fromFile(f)
      val parsedJson: JsValue = Json.parse(source.mkString)
      val id = PlaceController.generateID
      val name = getJsonProperty(parsedJson, "name")
      val country = getJsonProperty(parsedJson, "country")
      val description = getJsonProperty(parsedJson, "description")
      val picture = new File(getJsonProperty(parsedJson, "picture"))
      source.close()

      placeController.create(id, name, country, description, picture)
    }
  }

  /**
    * Clears database at application shutdown.
    */
  def onShutdown() = placeController.drop()



  // TODO get the flash scope to work
  def deletePlace(id: Int) = Action.async { implicit request =>
    placeController.remove(id).map {
      case true => Redirect(routes.Application.showGridView()).flashing("success" -> s"Deleted place with ID $id")
      case false => Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not delete place with ID $id")
    }
  }

  def editPlace(id: Int) = Action.async { implicit request =>
    placeController.findById(id).map {
      case Some(placeFound) =>
        val placeData = PlaceData(Some(id), placeFound.name, placeFound.country, placeFound.description)
        Ok(views.html.placeForm(PlaceController.createPlaceForm.fill(placeData)))
      case None => Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not find place with $id")
    }
  }

  def fileNotFound() = Action.async {implicit request => Future(NotFound(views.html.notFound()))}

  def getPictureOfPlace(id: Int) = Action.async {
    placeController.findById(id).map {
      case Some(place) => Ok(place.picture);
      case None => NotFound(s"Could not find picture for place with ID $id")
    }
  }

  def showGridView() = Action.async { implicit request =>
    placeController.getAllPlaces.map(placesList => {
      val numColumns = 3
      val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
      Ok(views.html.grid(placesList, numRows, numColumns))
    })
  }

  def showListView = Action.async {implicit request => placeController.getAllPlaces.map(placesList => Ok(views.html.list(placesList)))}


  // TODO get flash scope to work
  def showPlace(id: Int) = Action.async { implicit request =>
    placeController.findById(id).map {
      case Some(place) => Ok(views.html.showPlace(place))
      case None => Redirect(routes.Application.showGridView()).flashing("error" -> s"Cannot find place with id $id")
    }
  }

  def showPlaceForm = Action {implicit request => Ok(views.html.placeForm(PlaceController.createPlaceForm))}


  // TODO find out how to make the picture a required field for a newly created Place
  /**
    * Handles the form post. It either inserts a Place into the database or edits an existing Place depending on whether
    * the Place already has an ID (only existing Places have an ID).
    */
  def uploadPlace() = Action.async(parse.multipartFormData) { implicit request =>
    def failure(formWithErrors: Form[PlaceData]) = {
      Future(BadRequest(views.html.placeForm(formWithErrors)))
    }

    def success(placeData: PlaceData) = {
      val writeResultFuture = placeData.id match {
        case Some(id) => placeController.update(placeData, request.body.file("picture"))
        case None => placeController.create(placeData, request.body.file("picture"))
      }

      writeResultFuture.map {
        case w: UpdateWriteResult =>
          val flashMessage = if (w.ok) "success" -> "Successfully edited place" else "error" -> s"Could not edit place with id ${placeData.id.get}"
          Redirect(routes.Application.showGridView()).flashing(flashMessage)
        case w: WriteResult =>
          val flashMessage = if (w.ok) "success" -> "Successfully added place" else "error" -> "Could not add place to database"
          Redirect(routes.Application.showGridView()).flashing(flashMessage)
        case _ => throw new IllegalArgumentException
      }
    }

    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(failure, success)
  }
}

