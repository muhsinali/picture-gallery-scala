package models

import play.api.libs.json.Json

/**
  * Created by Muhsin Ali on 29/09/2016.
  */

/**
  * Used to gather information submitted in the form
  */
case class PlaceData(name: String, country: String, description: String)

/**
  * Place - a point of interest that the user would like to store in the gallery.
  */
case class Place (id: Int, name: String, country: String, description: String, picture: Array[Byte]) {
  override def toString: String = Integer.toString(id)
}

object Place {
  implicit val formatter = Json.format[Place]
}
