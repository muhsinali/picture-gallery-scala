package daos

import java.io.File
import java.sql.Date
import java.time.LocalDate

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.JpegWriter
import models.Place

import scala.collection.JavaConverters._


/**
  * S3DAO - the DAO for accessing images from an S3 bucket on AWS.
  *
  */
class S3DAO(val bucketName: String) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2)
      .build()


  // Uploads all images relevant to Place object
  def uploadImages(place: Place, imageToUpload: File): Unit = {
    implicit val writer = JpegWriter.NoCompression.withProgressive(true)

    // Generates thumbnails and uploads them to S3 bucket
    def uploadThumbnail(thumbnailKey: String, width: Int, height: Int): Unit = {
      val inputStream = Image.fromFile(imageToUpload).cover(width, height).stream
      val metadata = new ObjectMetadata()
      metadata.setContentLength(inputStream.available())
      metadata.setCacheControl("max-age = 31536000")
      s3.putObject(new PutObjectRequest(bucketName, thumbnailKey, inputStream, metadata)
          .withCannedAcl(CannedAccessControlList.PublicReadWrite))
    }

    uploadThumbnail(place.gridThumbnailKey, Place.gridThumbnailDims._1, Place.gridThumbnailDims._2)
    uploadThumbnail(place.listThumbnailKey, Place.listThumbnailDims._1, Place.listThumbnailDims._2)

    val inputStream = Image.fromFile(imageToUpload).stream
    val metadata = new ObjectMetadata()
    metadata.setContentLength(inputStream.available())
    metadata.setCacheControl("max-age = 31536000")
    s3.putObject(new PutObjectRequest(bucketName, place.pictureKey, inputStream, metadata)
        .withCannedAcl(CannedAccessControlList.PublicReadWrite))
  }


  def deleteImages(place: Place): Unit = s3.deleteObjects(new DeleteObjectsRequest(bucketName)
      .withKeys(place.pictureKey, place.gridThumbnailKey, place.listThumbnailKey))


  def getImage(key: String): S3Object = s3.getObject(bucketName, key)


  // Deletes all objects in bucket (note: the bucket itself isn't deleted)
  def emptyBucket(): Unit = {
    var objectListing = s3.listObjects(bucketName)
    while(true){
      val objectSummaries: Iterable[S3ObjectSummary] = objectListing.getObjectSummaries.asScala
      objectSummaries.foreach(obj => s3.deleteObject(bucketName, obj.getKey))

      if(!objectListing.isTruncated) return
      objectListing = s3.listNextBatchOfObjects(objectListing)
    }
  }
}
