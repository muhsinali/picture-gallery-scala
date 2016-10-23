import org.scalatestplus.play._
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

  }


}
