package daos

import java.io.File
import java.util.UUID

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

  val urlPrefix = s"https://$bucketName.s3.amazonaws.com"



  // TODO refactor this
  // TODO ensure that your set the content length to ObjectMetadata
  // TODO figure out how to speed this up - use Promises?
  def uploadFile(name: String, uuid: UUID, file: File): Unit = {
    val keyPrefix = name.toLowerCase.replace(" ", "-")
    val key = s"$keyPrefix-${uuid.toString}.jpg"
    s3.putObject(new PutObjectRequest(bucketName, key, file).withCannedAcl(CannedAccessControlList.PublicReadWrite))

    implicit val writer = JpegWriter(compression = 100, progressive = true)

    val gridThumbnailKey = s"$keyPrefix-grid-thumbnail-${uuid.toString}.jpg"
    val gridThumbnailInputStream = Image.fromFile(file).cover(360, 240).stream
    val metadata: ObjectMetadata = new ObjectMetadata()
    //val length: Long = IOUtils.toByteArray(gridThumbnailInputStream).length
    //metadata.setContentLength(length)
    s3.putObject(new PutObjectRequest(bucketName, gridThumbnailKey, gridThumbnailInputStream, metadata)
        .withCannedAcl(CannedAccessControlList.PublicReadWrite))

    val listThumbnailKey = s"$keyPrefix-list-thumbnail-${uuid.toString}.jpg"
    val listThumbnailInputStream = Image.fromFile(file).cover(75, 50).stream
    s3.putObject(new PutObjectRequest(bucketName, listThumbnailKey, listThumbnailInputStream, new ObjectMetadata())
        .withCannedAcl(CannedAccessControlList.PublicReadWrite))

  }


  def deleteFile(place: Place): Unit = s3.deleteObjects(new DeleteObjectsRequest(bucketName)
      .withKeys(place.pictureKey, place.gridThumbnailKey, place.listThumbnailKey))


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
