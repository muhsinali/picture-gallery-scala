package services

import java.io.File
import javax.inject.{Inject, Singleton}

import controllers.PlaceDAO
import play.Environment
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext
import scala.io.Source

/**
  * Responsible for setting up/tearing down the application.
  */
@Singleton
class ApplicationInterceptor @Inject() (reactiveMongoApi: ReactiveMongoApi, env: Environment,
                                        lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext){

  val placeController = new PlaceDAO(reactiveMongoApi)
  onStartup()
  lifecycle.addStopHook(() => onShutdown())


  /**
    * Populates database with Places at application startup.
    */
  def onStartup() = {
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

    // TODO might be handy to use parsedJson.as[Place] here - but Place.picture is of type Array[Byte]
    val jsonFiles = getListOfFiles("public/jsonFiles")
    for(f <- jsonFiles) {
      val source = Source.fromFile(f)
      val parsedJson: JsValue = Json.parse(source.mkString)
      val id = PlaceDAO.generateID
      val name = getJsonProperty(parsedJson, "name")
      val country = getJsonProperty(parsedJson, "country")
      val description = getJsonProperty(parsedJson, "description")
      val picture = new File(getJsonProperty(parsedJson, "picture"))
      source.close()

      placeController.create(id, name, country, description, picture)
    }
  }

  /**
    * Clears database at application shutdown.
    */
  def onShutdown() = {
    Logger.info("Clearing database on shutdown")
    placeController.drop()
  }
}
