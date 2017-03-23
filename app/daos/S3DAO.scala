package daos

import java.io.File
import java.util.UUID

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest, S3ObjectSummary}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import models.Place

import scala.collection.JavaConverters._

class S3DAO(val bucketName: String) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2).build()


  val urlPrefix: String = s"https://$bucketName.s3.amazonaws.com"

  // TODO also upload grid and thumbnail sized images
  def uploadFile(name: String, uuid: UUID, file: File): Unit = {
    val key = s"${name.toLowerCase.replace(" ", "-")}-${uuid.toString}.jpg"
    s3.putObject(new PutObjectRequest(bucketName, key, file).withCannedAcl(CannedAccessControlList.PublicReadWrite))
  }

  // TODO also delete grid and thumbnail sized images
  def deleteFile(place: Place): Unit = {
    s3.deleteObject(bucketName, place.pictureKey)
  }


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
