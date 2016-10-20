package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.util.{Success, Failure}


// TODO fix upload method
// TODO add flashing where appropriate
// TODO implement edit and delete functionality

/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {
  implicit val formatter = Json.format[Place]

  val placeController = new PlaceController(reactiveMongoApi)

  def index = Action { Redirect(routes.Application.showGrid())}

  def showList() = Action.async { implicit request =>
    placeController.retrieveAllPlaces().map(placesList => Ok(views.html.list(placesList)))
  }

  def showGrid() = Action.async { implicit request =>
    placeController.retrieveAllPlaces().map(placesList => {
      val numColumns = 3
      val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
      Ok(views.html.grid(placesList, numRows, numColumns))
    })
  }

  def showPlace(id: Int) = Action.async { implicit request =>
    for {
      // TODO move into PlaceController
      places <- placeController.placesFuture
      placeOpt <- places.find(Json.obj("id" -> id)).one[Place]
    } yield {
      if (placeOpt.isDefined) {
        Ok (views.html.showPlace (placeOpt.get))
      } else {
        Redirect(routes.Application.index()).flashing("error" -> s"Cannot find place with id $id")
      }
    }
  }


  def upload = Action.async(parse.multipartFormData) { implicit request =>
    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        // TODO improve this - add flash scope
        Future(BadRequest(views.html.placeForm(formWithErrors)))
      },
      placeData => {
        // TODO use writeResult and return error if couldn't be saved to database
        val writeResult = placeController.create(placeData, request.body.file("picture").get)
        writeResult.map(w => {
          // TODO flash scope not showing up
          if(!w.hasErrors){
            Redirect(routes.Application.index()).flashing("success" -> "Successfully added Place")
          } else {
            Redirect(routes.Application.index()).flashing("error" -> "Could not add Place to database")
          }
        })
      }
    )
  }

  def displayPlaceForm() = Action {implicit request =>
    Ok(views.html.placeForm(PlaceController.createPlaceForm))
  }

}

