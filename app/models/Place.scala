package models

import java.util.UUID

import play.api.libs.json.{Json, OFormat}

/**
  * PlaceData
  * Used to gather information submitted in a form. This case class is used to decouple form submission and
  * validation from the corresponding model class.
  */
case class PlaceData(id: Option[Int], name: String, country: String, description: String)



/**
  * Place - a point of interest that the user would like to store in the database.
  *
  * s3BucketName    the AWS S3 bucket that contains the images for this Place object
  * s3Uuid          the UUID used in the filenames of the images for this Place in the relevant S3 bucket
  */
case class Place (id: Int, name: String, country: String, description: String, s3BucketName: String, s3Uuid: UUID) {
  val urlPrefix: String = s"https://$s3BucketName.s3.amazonaws.com"

  val gridThumbnailKey: String = s"${name.toLowerCase.replace(" ", "-")}-grid-thumbnail-$s3Uuid.jpg"
  val listThumbnailKey: String = s"${name.toLowerCase.replace(" ", "-")}-list-thumbnail-$s3Uuid.jpg"
  val pictureKey: String = s"${name.toLowerCase.replace(" ", "-")}-$s3Uuid.jpg"

  val gridThumbnailUrl: String = s"$urlPrefix/$gridThumbnailKey"
  val listThumbnailUrl: String = s"$urlPrefix/$listThumbnailKey"
  val pictureUrl: String = s"$urlPrefix/$pictureKey"


  def this(pd: PlaceData, s3BucketName: String, s3Uuid: UUID){
    this(pd.id.get, pd.name, pd.country, pd.description, s3BucketName, s3Uuid)
  }

  override def toString: String = id.toString
}



object Place {
  implicit val formatter: OFormat[Place] = Json.format[Place]

  // Dimensions = (width, height)
  val gridThumbnailDims: (Int, Int) = (360, 240)
  val listThumbnailDims: (Int, Int) = (75, 50)
}
