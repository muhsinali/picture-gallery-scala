package controllers

import javax.inject.Inject

import com.google.common.io.Files
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._


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
    placeController.placesFuture.flatMap(_.find(Json.obj("id" -> id)).one[Place]).map(placeOpt => {
      if(placeOpt.isDefined){
        Ok(views.html.showPlace(placeOpt.get))
      } else {
        BadRequest("Uh oh")
      }
    })
  }

  def upload = Action.async(parse.multipartFormData) { implicit request =>
    val boundForm = PlaceController.createPlaceForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        Future(BadRequest(views.html.placeForm(formWithErrors)))
      },
      placeData => {
        placeController.placesFuture.flatMap(places => {
          request.body.file("picture").map { picture =>
            places.insert(Place(0, placeData.name, placeData.country, placeData.description, Files.toByteArray(picture.ref.file)))
            Future(Redirect(routes.Application.index()).flashing("success" -> s"Added place ${placeData.name}"))
          }.getOrElse {
            println("Something went wrong...")
            Future(Redirect(routes.Application.index()).flashing("error" -> "Could not upload place. Please correct the form below."))
          }
        })
      }
    )
  }

  def displayPlaceForm() = Action {implicit request =>
    Ok(views.html.placeForm(PlaceController.createPlaceForm))
  }

}

