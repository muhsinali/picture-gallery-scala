package controllers

import javax.inject.Inject

import daos.PlaceDAO
import models.{Place, PlaceData}
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.ApplicationLifecycle
import play.api.libs.Files
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, Controller, MultipartFormData}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}

import scala.concurrent.{ExecutionContext, Future}


/**
  * Application is the entry point of this web application and handles all HTTP requests for this web application.
  */
class Application @Inject()(val messagesApi: MessagesApi, val reactiveMongoApi: ReactiveMongoApi,
                            applicationLifecycle: ApplicationLifecycle, config: Configuration)
                           (implicit ec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  implicit val formatter: OFormat[Place] = Json.format[Place]
  val placeDAO = new PlaceDAO(reactiveMongoApi, config)


  // TODO get the flash scope to work
  def deletePlace(id: Int): Action[AnyContent] = Action.async { implicit request =>
    placeDAO.remove(id).map {
      case true => Redirect(routes.Application.showGridView()).flashing("success" -> s"Deleted place with ID $id")
      case false => Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not delete place with ID $id")
    }
  }

  def editPlace(id: Int): Action[AnyContent] = Action.async { implicit request =>
    placeDAO.findById(id).map {
      case Some(placeFound) =>
        val placeData = PlaceData(Some(id), placeFound.name, placeFound.country, placeFound.description)
        Ok(views.html.placeForm(PlaceDAO.createPlaceForm.fill(placeData), Some(placeFound.pictureUrl)))
      case None => Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not find place with ID $id")
    }
  }

  def fileNotFound(): Action[AnyContent] = Action{implicit request => NotFound(views.html.notFound())}

  //def index(): Action[AnyContent] = Action{implicit request => Redirect(routes.Application.showGridView())}

  def showGridView(): Action[AnyContent] = Action.async { implicit request =>
    placeDAO.getAllPlaces.map(placesList => {
      val numColumns = 3
      val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
      Ok(views.html.grid(placesList, numRows, numColumns))
    })
  }

  def showListView: Action[AnyContent] = Action.async {implicit request =>
    placeDAO.getAllPlaces.map(placesList => Ok(views.html.list(placesList)))
  }


  // TODO get flash scope to work
  def showPlace(id: Int): Action[AnyContent] = Action.async { implicit request =>
    placeDAO.findById(id).map {
      case Some(place) => Ok(views.html.showPlace(place))
      case None => Redirect(routes.Application.showGridView()).flashing("error" -> s"Cannot find place with id $id")
    }
  }

  def showPlaceForm: Action[AnyContent] = Action {implicit request => Ok(views.html.placeForm(PlaceDAO.createPlaceForm, None))}


  // TODO find out how to make the picture a required field for a newly created Place
  /**
    * Handles the form post. It either inserts a Place into the database or edits an existing Place depending on whether
    * the Place already has an ID (only existing Places have an ID).
    */
  def uploadPlace(): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    def failure(formWithErrors: Form[PlaceData]) = {
      Future(BadRequest(views.html.placeForm(formWithErrors, None)))
    }

    def success(placeData: PlaceData) = {
      val writeResultFuture = placeData.id match {
        case Some(_) => placeDAO.update(placeData, request.body.file("picture"))
        case None => placeDAO.create(placeData, request.body.file("picture"))
      }

      writeResultFuture.map {
        case w: UpdateWriteResult =>
          val flashMessage = if (w.ok) "success" -> "Successfully edited place"
            else "error" -> s"Could not edit place with id ${placeData.id.get}"
          Redirect(routes.Application.showGridView()).flashing(flashMessage)
        case w: WriteResult =>
          val flashMessage = if (w.ok) "success" -> "Successfully added place" else "error" -> "Could not add place to database"
          Redirect(routes.Application.showGridView()).flashing(flashMessage)
        case _ => throw new IllegalArgumentException
      }
    }

    PlaceDAO.createPlaceForm.bindFromRequest().fold(failure, success)
  }
}

