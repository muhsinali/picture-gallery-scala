package daos

import java.io.{File, FileOutputStream, InputStream}
import javax.inject.Inject

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.apache.commons.io.IOUtils
import play.api.Configuration


// Used simply to download the relevant images at application startup
class ApplicationStartupS3DAO @Inject()(config: Configuration) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2)
      .build()
  private val bucketName = config.underlying.getString("aws-static-s3")

  def downloadImageAsTempFile(key: String): File = {
    val inputStream: InputStream = s3.getObject(bucketName, key).getObjectContent
    val tempFile = File.createTempFile(s"temp-file-$key", ".tmp")
    tempFile.deleteOnExit()
    val out = new FileOutputStream(tempFile)
    IOUtils.copy(inputStream, out)
    inputStream.close()
    out.close()
    tempFile
  }
}
