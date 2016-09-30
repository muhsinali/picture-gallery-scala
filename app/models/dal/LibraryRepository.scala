package models.dal

import models.Place

/**
  * Created by Muhsin Ali on 29/09/2016.
  */
object LibraryRepository {
  var placesList: List[Place] = List(
    new Place(1, "London", "United Kingdom"),
    new Place(2, "Paris", "France"),
    new Place(3, "Dubai", "United Arab Emirates"),
    new Place(4, "Singapore", "Singapore"),
    new Place(5, "Tokyo", "Japan")
  )
}
