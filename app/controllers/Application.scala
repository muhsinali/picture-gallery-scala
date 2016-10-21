package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.{Place, PlaceData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import scala.concurrent.{ExecutionContext, Future}


// TODO implement edit and delete functionality

/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application @Inject()(val messagesApi: MessagesApi, val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  implicit val formatter = Json.format[Place]
  val placeController = new PlaceController(reactiveMongoApi)

  // TODO get the flash scope to work
  def deletePlace(id: Int) = Action.async { implicit request =>
    placeController.remove(id).map(wasPlaceRemoved => {
      if(wasPlaceRemoved){
        Redirect(routes.Application.showGridView()).flashing("success" -> s"Deleted place with ID $id")
      } else {
        Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not delete place with ID $id")
      }
    })
  }

  // TODO find out how to fill the picture field. It is not part of the PlaceData class.
  def editPlace(id: Int) = Action.async { implicit request =>
    placeController.findById(id).map(placeOpt => {
      if(placeOpt.isDefined){
        val placeFound = placeOpt.get
        val placeData = PlaceData(placeFound.name, placeFound.country, placeFound.description)
        Ok(views.html.placeForm(PlaceController.createPlaceForm.fill(placeData)))
      } else {
        Redirect(routes.Application.showGridView()).flashing("error" -> s"Could not find place with $id to edit")
      }
    })
  }

  def showGridView() = Action.async { implicit request =>
    placeController.retrieveAllPlaces.map(placesList => {
      val numColumns = 3
      val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
      Ok(views.html.grid(placesList, numRows, numColumns))
    })
  }

  def retrievePictureOfPlace(id: Int) = Action.async {
    placeController.findById(id).map(placeOpt => {
      if(placeOpt.isDefined){
        Ok(placeOpt.get.picture)
      } else {
        BadRequest(s"Could not find picture for Place with ID $id")
      }
    })
  }

  def showListView = Action.async { implicit request =>
    placeController.retrieveAllPlaces.map(placesList => Ok(views.html.list(placesList)))
  }

  def showPlace(id: Int) = Action.async { implicit request =>
    placeController.findOne(Json.obj("id" -> id)).map(placeOpt => {
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
  // Handles the form post, inserting the new Place into the MongoDB database
  def uploadPlace() = Action.async(parse.multipartFormData) { implicit request =>
    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        Future(BadRequest(views.html.placeForm(formWithErrors)))
      },
      placeData => {
        val writeResultFuture = placeController.create(placeData, request.body.file("picture").get)
        writeResultFuture.map(writeResult => {
          if(!writeResult.hasErrors){
            Redirect(routes.Application.showGridView()).flashing("success" -> "Successfully added Place")
          } else {
            Redirect(routes.Application.showGridView()).flashing("error" -> "Could not add Place to database")
          }
        })
      }
    )
  }
}

