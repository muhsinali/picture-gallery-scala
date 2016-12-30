package models

import play.api.libs.json.Json   // implicit formatter for BSONObjectID

/**
  * Created by Muhsin Ali on 29/09/2016.
  */

/**
  * PlaceData - Used to gather information submitted in a form.
  */
case class PlaceData(id: Option[Int], name: String, country: String, description: String)



/**
  * Place - a point of interest that the user would like to store in the gallery.
  */
case class Place (id: Int, name: String, country: String, description: String, picture: Array[Byte]) {
  override def toString: String = id.toString
}

object Place {
  implicit val formatter = Json.format[Place]
}
