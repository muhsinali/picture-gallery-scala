package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONObjectIDFormat   // implicit format for BSONObjectID

/**
  * Created by Muhsin Ali on 29/09/2016.
  */

/**
  * Used to gather information submitted in the form
  */
case class PlaceData(name: String, country: String, description: String)

/**
  * Place - a point of interest that the user would like to store in the gallery.
  *
  * _id is declared in the constructor for the ObjectID to be stored under the "_id" field in the MongoDB database.
  */
case class Place (private val _id: BSONObjectID, name: String, country: String, description: String, picture: Array[Byte]) {
  val id = _id
  override def toString: String = id.toString
}

object Place {
  implicit val formatter = Json.format[Place]
}
