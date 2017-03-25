package services

import java.io.{File, FileOutputStream, InputStream}
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import daos.PlaceDAO
import org.apache.commons.io.IOUtils
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
  * Responsible for setting up/tearing down the application.
  */
@Singleton
class ApplicationInterceptor @Inject() (reactiveMongoApi: ReactiveMongoApi, config: Configuration,
                                        lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext){

  val placeDAO = new PlaceDAO(reactiveMongoApi, config)
  onStartup()
  lifecycle.addStopHook(() => onShutdown())


  /**
    * Populates database with Places at application startup.
    */
  def onStartup(): Unit = {
    // Get a list of all the files in a directory
    def getListOfFiles(dirPath: String) = {
      val dir = new File(dirPath)
      if(dir.exists() && dir.isDirectory){
        dir.listFiles.filter(f => f.isFile && f.getPath.endsWith(".json")).toList
      } else {
        List[File]()
      }
    }

    def getJsonProperty(jsValue: JsValue, field: String) = (jsValue \ field).as[String].replace("\"", "")

    Logger.info("Populating database with tasks on startup")

    val startupS3 = new ApplicationStartupS3DAO(config)
    val jsonFiles = getListOfFiles("public/jsonFiles")
    for(f <- jsonFiles) {
      val source = Source.fromFile(f)
      val parsedJson: JsValue = Json.parse(source.mkString)
      val id = PlaceDAO.generateID
      val name = getJsonProperty(parsedJson, "name")
      val country = getJsonProperty(parsedJson, "country")
      val description = getJsonProperty(parsedJson, "description")
      val picture = startupS3.downloadImageAsTempFile(getJsonProperty(parsedJson, "picture"))
      source.close()
      placeDAO.create(id, name, country, description, picture)
      picture.delete()
    }
  }

  /**
    * Clears database at application shutdown.
    */
  def onShutdown(): Future[Boolean] = {
    Logger.info("Clearing database on shutdown")
    placeDAO.drop()
  }
}


// Used simply to download the relevant images at application startup
class ApplicationStartupS3DAO @Inject()(config: Configuration) {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withRegion(Regions.EU_WEST_2)
      .build()
  private val bucketName = config.underlying.getString("aws-static-s3")

  private[services] def downloadImageAsTempFile(key: String): File = {
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
