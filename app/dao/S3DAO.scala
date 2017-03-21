package dao

import java.io.File

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

class S3DAO(val bucketName: String) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2).build()


  def uploadFile(file: File, key: String): Unit = {
    s3.putObject(new PutObjectRequest(bucketName, key, file))
  }
}
