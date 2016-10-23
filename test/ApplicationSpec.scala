import java.io.File

import com.google.common.io.Files
import models.Place
import org.scalatestplus.play._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerSuite {

  "Routes" should {
    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }
  }

  "Application" should {
    // Application.showGridView tests
    "render the grid view" in {
      val gridView = route(app, FakeRequest(GET, "/")).get
      status(gridView) mustBe OK
      contentAsString(gridView) must include ("Grid view")
    }

    // Application.showListView tests
    "render the list view" in {
      val listView = route(app, FakeRequest(GET, "/list")).get
      status(listView) mustBe OK
      contentAsString(listView) must include ("List view")
    }

    // Application.showPlace() tests
    "show London place" in {
      val london = route(app, FakeRequest(GET, "/show/1")).get
      status(london) mustBe OK
      contentAsString(london) must include ("London")
    }
    "fail on non-existent place" in {
      val nonExistentPlace = route(app, FakeRequest(GET, "/show/9999")).get
      status(nonExistentPlace) mustBe SEE_OTHER
    }

    // Application.showPlaceForm() tests
    "render the Place form" in {
      val placeForm = route(app, FakeRequest(GET, "/add")).get
      status(placeForm) mustBe OK
    }

    // Application.getPictureOfPlace() tests
    "get picture of London" in {
      val pictureOfLondon = route(app, FakeRequest(GET, "/picture/1")).get
      status(pictureOfLondon) mustBe OK
    }
    "fail on non-existent picture" in {
      val pictureOfNonExistentPlace = route(app, FakeRequest(GET, "/picture/9999")).get
      status(pictureOfNonExistentPlace) mustBe BAD_REQUEST
    }


    // TODO add a mock place, and edit it and then delete it.
//    // Doesn't work - TODO figure out how to add a mock place
//    "add a mock place" in {
//      val placeData: Map[String, Seq[String]] = Map(
//        "id" -> Seq("0"),
//        "name" -> Seq("mockName"),
//        "country" -> Seq("mockCountry"),
//        "description" -> Seq("mockDescription")
//      )
//
//      val tempFile = TemporaryFile(new File("./public/images/favicon.png"))
//      val part = FilePart[TemporaryFile]("picture", "./public/images/favicon.png", Some("image/jpeg"), tempFile)
//      val formData = MultipartFormData(dataParts = placeData, files = Seq(part), badParts = Seq())
//      val addResult = route(FakeRequest(POST, "/add", FakeHeaders(), formData)).get
//
//
//      println(addResult)
//      status(addResult) mustBe SEE_OTHER
//    }


    // Application.editPlace() tests
    "edit London place" in {
      val editLondon = route(app, FakeRequest(GET, "/edit/1")).get
      status(editLondon) mustBe OK
    }

    // Application.deletePlace() tests
    "delete London place" in {
      val deleteLondon = route(app, FakeRequest(DELETE, "/delete/1")).get
      status(deleteLondon) mustBe SEE_OTHER
      // TODO find out why deleteLondon is empty
//      println(contentAsString(deleteLondon))
//      contentAsString(deleteLondon) must include ("Deleted place with ID")
    }

  }


}
