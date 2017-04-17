Picture Gallery
=================================
![alt tag](public/images/pictureGallery.png)

### [Try out project here](http://gallery.muhsinali.xyz)

#### [WebPageTest performance results](https://www.webpagetest.org/result/170331_19_6DGB/1/performance_optimization/)

This is a RESTful, CRUD web application that stores places of interest in a database and displays them either using a grid or a list layout back to the user.

The user can add, edit or delete places from the database. It uses the [Play framework](https://www.playframework.com/) and the MVC pattern.


### Key features
1. Used GitFlow and set up a continuous deployment pipeline using [Travis](https://travis-ci.org/muhsinali/picture-gallery-scala) and Heroku
2. A 404 page is [displayed](https://github.com/muhsinali/picture-gallery-scala/blob/c3b4a00425caf0c3e65f3bd64f3dcb28fe02ff93/app/ErrorHandler.scala#L14-L20) whenever the user attempts to access a non-existent page
3. Example Places are [programmatically loaded](https://github.com/muhsinali/picture-gallery-scala/blob/37cb376bca11feaef761ec8ea7576753b7129538/app/services/ApplicationInterceptor.scala#L30-L59) into the database at application startup
4. Static assets are stored using Amazon S3 and served using Amazon CloudFront
5. Thumbnails of all images uploaded by the user are [programmatically generated](https://github.com/muhsinali/picture-gallery-scala/blob/37cb376bca11feaef761ec8ea7576753b7129538/app/daos/S3DAO.scala#L29-L52) for both the grid and list views using [Scrimage](https://github.com/sksamuel/scrimage)
6. All images that are uploaded by the web app to an Amazon S3 bucket are first converted to [progressive JPEGs](https://github.com/muhsinali/picture-gallery-scala/blob/37cb376bca11feaef761ec8ea7576753b7129538/app/daos/S3DAO.scala#L31) to improve the web app's perceived performance
7. Made use of [WebPageTest](https://www.webpagetest.org/result/170331_19_6DGB/1/performance_optimization/) to look for other performance improvements. Subsequently:
    * Added a [GZip compression filter](https://github.com/muhsinali/picture-gallery-scala/blob/master/app/Filters.scala) to gzip server responses
    * Made use of browser caching


### Tech stack
- Scala 2.11.8
- Play framework (version 2.5.12)
- MongoDB (ReactiveMongo 0.12.1)
- HTML, CSS, Bootstrap 3, [Twirl template engine](https://www.playframework.com/documentation/2.5.x/ScalaTemplates)