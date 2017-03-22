package models

import play.api.libs.json.Json

/**
  * PlaceData
  * Used to gather information submitted in a form. This case class is used to decouple form submission and
  * validation from the corresponding model class.
  */
case class PlaceData(id: Option[Int], name: String, country: String, description: String)



/**
  * Place - a point of interest that the user would like to store in the database.
  */
case class Place (id: Int, name: String, country: String, description: String, picture: String, pictureUrl: String) {
  override def toString: String = id.toString
}

object Place {
  implicit val formatter = Json.format[Place]
}
