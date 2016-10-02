package models

import play.api.libs.json.Json

/**
  * Created by Muhsin Ali on 29/09/2016.
  */

case class Place (id: Int, name: String, country: String)

object Place {
  implicit val formatter = Json.format[Place]
}
