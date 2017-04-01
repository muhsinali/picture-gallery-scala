Picture Gallery
=================================
![alt tag](public/images/pictureGallery.png)

### [Try out project here](http://gallery.muhsinali.xyz)

#### [WebPageTest performance results](https://www.webpagetest.org/result/170331_19_6DGB/1/performance_optimization/)

This is a RESTful, CRUD web application that stores places of interest in a database and displays them either using a grid or a list layout back to the user.

The user can add, edit or delete places from the database. It uses the [Play framework](https://www.playframework.com/) and the MVC pattern, and is currently configured to run locally on one's machine.

There is also a Java implementation of this project [here](https://github.com/muhsinali/picture-gallery-java) (rewrote it in Scala so I could learn Scala :grinning:).


### Tech stack
- Scala 2.11.8
- Play framework (version 2.5.12)
- MongoDB (ReactiveMongo 0.12.1)
- HTML, CSS, Bootstrap 3, [Twirl template engine](https://www.playframework.com/documentation/2.5.x/ScalaTemplates)


### Key features
1. Used GitFlow and set up a continuous deployment pipeline using Travis and Heroku
2. Static assets are stored using Amazon S3 and served using Amazon CloudFront
3. Thumbnails of all images uploaded by the user are programmatically generated for both the grid and list views using [Scrimage](https://github.com/sksamuel/scrimage)
4. All images that are uploaded by the web app to an Amazon S3 bucket are first converted to progressive JPEGs to improve the web app's perceived performance
5. Made use of [WebPageTest](https://www.webpagetest.org/) to look for other performance improvements
    * Added a GZip compression filter to compress server responses
    * Used browser caching