package models

import play.api.libs.json.{Json, OFormat}

/**
  * PlaceData
  * Used to gather information submitted in a form. This case class is used to decouple form submission and
  * validation from the corresponding model class.
  */
case class PlaceData(id: Option[Int], name: String, country: String, description: String)



/**
  * Place - a point of interest that the user would like to store in the database.
  */
case class Place (id: Int, name: String, country: String, description: String, urlPrefix: String, s3Uuid: String) {
  def this(pd: PlaceData, urlPrefix: String, s3Uuid: String){
    this(pd.id.get, pd.name, pd.country, pd.description, urlPrefix, s3Uuid)
  }


  // TODO remove key and url fields once ready
  val gridThumbnailKey: String = s"${name.toLowerCase.replace(" ", "-")}-grid-thumbnail-$s3Uuid.jpg"
  val listThumbnailKey: String = s"${name.toLowerCase.replace(" ", "-")}-list-thumbnail-$s3Uuid.jpg"
  val pictureKey: String = s"${name.toLowerCase.replace(" ", "-")}-$s3Uuid.jpg"

  val gridThumbnailUrl: String = s"$urlPrefix/$gridThumbnailKey"
  val listThumbnailUrl: String = s"$urlPrefix/$listThumbnailKey"
  val pictureUrl: String = s"$urlPrefix/$pictureKey"

  override def toString: String = id.toString
}

object Place {
  implicit val formatter: OFormat[Place] = Json.format[Place]
}
