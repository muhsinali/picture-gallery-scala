package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Result}
import models.{Place, PlaceData}
import play.api.data.Form
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._


/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents {
  implicit val formatter = Json.format[Place]

  val placeController = new PlaceController(reactiveMongoApi)

    def index = Action { Redirect("/grid")}

  // TODO this is blocking, find out how to make it non-blocking
  def showList() = Action { implicit request =>
    Ok(views.html.list(Await.result(placeController.retrieveAllPlaces(), 1 seconds)))
  }

  // TODO this is blocking, find out how to make it non-blocking
  def showGrid() = Action { implicit request =>
    val placesList = Await.result(placeController.retrieveAllPlaces(), 1 seconds)
    val numColumns = 3
    val numRows = math.ceil(placesList.length / numColumns.toDouble).toInt
    Ok(views.html.grid(Await.result(placeController.retrieveAllPlaces(), 1 seconds), numRows, numColumns))
  }

  // TODO this is blocking, find out how to make it non-blocking
  def showPlace(id: Int) = Action { implicit request =>
    val placeOpt: Option[Place] = Await.result(placeController.placesFuture.flatMap(_.find(Json.obj("id" -> id)).one[Place]), 1 seconds)
    if(placeOpt.isDefined){
      Ok(views.html.showPlace(placeOpt.get))
    } else {
      BadRequest("Uh oh")
    }
  }

//  def upload() = Action { implicit request =>
//    val body = request.body.asMultipartFormData.get
//
//    val boundForm = placeController.placeForm.bindFromRequest()
//    var message = "Everything works"
//    if(boundForm.hasErrors){
//      message = "This form has errors!"
//      //return BadRequest(views.html.placeForm(boundForm))
//    }
//
//    val placeData = boundForm.get
//    println(s"${placeData.name}")
//    //val boundPictureURL = body.file("picture")
//
//
//
//    BadRequest(message)
//  }

  def upload = Action(parse.multipartFormData) { implicit request =>

    placeController.placeForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.placeForm(formWithErrors))
      },
      placeData => {
        Ok("this worked for some reason!")
      }
    )


//    request.body.file("picture").map { picture =>
//      import java.io.File
//      val filename = picture.filename
//      val contentType = picture.contentType
//      //picture.ref.moveTo(new File(s"/tmp/picture/$filename"))
//      Ok("File uploaded")
//    }.getOrElse {
//      Redirect(routes.Application.index()).flashing("error" -> "Missing file")
//    }
  }

  def displayPlaceForm() = Action {implicit request =>
    Ok(views.html.placeForm(placeController.placeForm))
  }

}

