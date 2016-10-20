package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
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
    for{
      // TODO move to PlaceController
      placeToDelete <- placeController.findById(id)
      places <- placeController.placesFuture
    }yield{
      if(placeToDelete.isDefined) {
        places.remove[Place](placeToDelete.get, firstMatchOnly = true)
        Redirect(routes.Application.index()).flashing("success" -> s"Deleted place with ID $id")
      } else {
        Redirect(routes.Application.index()).flashing("error" -> s"Could not delete place with ID $id")
      }
    }
  }

  def editPlace(id: Int) = TODO

  def index() = Action.async { implicit request =>
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

  def showList = Action.async { implicit request =>
    placeController.retrieveAllPlaces.map(placesList => Ok(views.html.list(placesList)))
  }

  def showPlace(id: Int) = Action.async { implicit request =>
    placeController.findOne(Json.obj("id" -> id)).map(placeOpt => {
      if (placeOpt.isDefined) {
        Ok (views.html.showPlace(placeOpt.get))
      } else {
        Redirect(routes.Application.index()).flashing("error" -> s"Cannot find place with id $id")
      }
    })
  }

  def showPlaceForm = Action { implicit request =>
    Ok(views.html.placeForm(PlaceController.createPlaceForm))
  }

  // TODO find out how to make the picture a required field
  // Handles the form post, inserting the new Place into the MongoDB database
  def upload() = Action.async(parse.multipartFormData) { implicit request =>
    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        Future(BadRequest(views.html.placeForm(formWithErrors)))
      },
      placeData => {
        val writeResult = placeController.create(placeData, request.body.file("picture").get)
        writeResult.map(w => {
          if(!w.hasErrors){
            Redirect(routes.Application.index()).flashing("success" -> "Successfully added Place")
          } else {
            Redirect(routes.Application.index()).flashing("error" -> "Could not add Place to database")
          }
        })
      }
    )
  }
}

