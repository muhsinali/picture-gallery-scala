package daos

import java.io.File

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest, S3ObjectSummary}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import scala.collection.JavaConverters._

class S3DAO(val bucketName: String) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2).build()


  def uploadFile(file: File, key: String): String = {
    s3.putObject(new PutObjectRequest(bucketName, key, file).withCannedAcl(CannedAccessControlList.PublicReadWrite))
    s3.getUrl(bucketName, key).toString
  }

  def deleteFile(key: String): Unit = {
    s3.deleteObject(bucketName, key)
  }


  def emptyBucket(): Unit = {
    var objectListing = s3.listObjects(bucketName)
    while(true){
      val objectSummaries: Iterable[S3ObjectSummary] = objectListing.getObjectSummaries.asScala
      objectSummaries.foreach(obj => deleteFile(obj.getKey))

      if(!objectListing.isTruncated) return
      objectListing = s3.listNextBatchOfObjects(objectListing)
    }
  }
}
