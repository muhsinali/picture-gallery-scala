package models

import play.api.libs.json.Json

/**
  * Created by Muhsin Ali on 29/09/2016.
  */

case class PlaceData(name: String, country: String, description: String, pictureURL: String)

case class Place (id: Int, name: String, country: String, description: String, picture: Array[Byte]) {
  override def toString: String = Integer.toString(id)
}

// TODO read up on this in more detail
object Place {
  implicit val formatter = Json.format[Place]
}
