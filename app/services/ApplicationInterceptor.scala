package services

import java.io.File
import javax.inject.{Inject, Singleton}

import daos.{ApplicationStartupS3DAO, PlaceDAO}
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
        dir.listFiles.filter(f => f.isFile && f.canRead && f.getPath.endsWith(".json")).toList
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
      val picture = startupS3.downloadImageToTempFile(getJsonProperty(parsedJson, "picture"))
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