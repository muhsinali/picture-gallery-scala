package daos

import java.io.File

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.JpegWriter
import models.Place

import scala.collection.JavaConverters._

class S3DAO(val bucketName: String) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2).build()

  // Uploads all images relevant to place object
  def uploadImages(place: Place, imageToUpload: File): Unit = {
    // Generates thumbnails and uploads them to bucket
    def uploadThumbnail(thumbnailKey: String, width: Int, height: Int): Unit = {
      implicit val writer = JpegWriter(compression = 100, progressive = true)
      val inputStream = Image.fromFile(imageToUpload).cover(width, height).stream
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentLength(inputStream.available())
      s3.putObject(new PutObjectRequest(bucketName, thumbnailKey, inputStream, metadata)
          .withCannedAcl(CannedAccessControlList.PublicReadWrite))
    }

    uploadThumbnail(place.gridThumbnailKey, 360, 240)
    uploadThumbnail(place.listThumbnailKey, 75, 50)
    s3.putObject(new PutObjectRequest(bucketName, place.pictureKey, imageToUpload)
        .withCannedAcl(CannedAccessControlList.PublicReadWrite))
  }


  def deleteFile(place: Place): Unit = s3.deleteObjects(new DeleteObjectsRequest(bucketName)
      .withKeys(place.pictureKey, place.gridThumbnailKey, place.listThumbnailKey))


  // Deletes all objects in bucket (the bucket itself isn't deleted)
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
