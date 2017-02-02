Introduction:
=================================
This is a RESTful, CRUD web application that stores places of interest in a database and displays them either using a grid or a list layout back to the user.

The user can add, edit or delete places from the database. It uses the [Play framework](https://www.playframework.com/) and the MVC pattern, and is currently configured to run locally on one's machine.

There is also a Java implementation of this project [here](https://github.com/muhsinali/picture-gallery) (rewrote it in Scala so I could learn Scala :grinning:).

###How to run the web app:
To run the web app locally, start the MongoDB database using `mongod` and then go to the root directory of this project and run `activator run`. Once ready, go to [http://localhost:9000](http://localhost:9000) (if running for the first time, will need to wait a bit for the source code to compile).

###Tech stack:
- Scala 2.11.8
- Play framework (version 2.5.8)
- MongoDB (ReactiveMongo 0.12)
- HTML, CSS, Bootstrap 3, [Twirl template engine](https://www.playframework.com/documentation/2.5.x/ScalaTemplates)




Improvements:
=================================

1. Application generally appears to be slow at runtime (e.g. delete request takes 5-10s when it should be in the milliseconds). It seems that the Play app is waiting to connect to the database. Not sure why; this does not occur in the Java implementation of this web app.

2. Would like to ensure that the user has uploaded a picture (i.e. I would like this to be a requirement using form validation).

3. Use Place objects in the routes file instead of Int, but not sure how to uniquely represent a Place object in a URL without using a GUID (not a good idea to expose the internals of the application in the URL).

4. Would like to improve the tests that I have currently written. Not sure how to include a mock object in a HTTP request that's then sent to the web app.

5. Getting flash messages to show up on some redirect requests, as they provide useful information to the user.

If you have any suggestions, please submit a feature request/issue/bug. Always looking for feedback! 