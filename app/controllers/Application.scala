package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.Place
import models.dal.LibraryRepository
import play.twirl.api.Html


/**
  * Created by Muhsin Ali on 29/09/2016.
  */
class Application extends Controller {
  implicit val formatter = Json.format[Place]

  def index = Action {implicit request =>
    val jsonInfo = Json.prettyPrint(Json.toJson(LibraryRepository.placesList))
    Ok(views.html.main("")(Html(jsonInfo)))
  }
}
