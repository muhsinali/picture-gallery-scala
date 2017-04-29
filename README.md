Introduction [![Build Status](https://travis-ci.org/muhsinali/picture-gallery-scala.svg?branch=master)](https://travis-ci.org/muhsinali/picture-gallery-scala) [![Apache 2.0 License](https://img.shields.io/badge/license-Apache2-green.svg) ](https://github.com/muhsinali/picture-gallery-scala/blob/master/LICENSE) [![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
=================================
![alt tag](public/images/pictureGallery.png)

### [Try out project here](http://gallery.muhsinali.xyz)

#### [WebPageTest performance results](https://www.webpagetest.org/result/170331_19_6DGB/5/performance_optimization/)

This is a RESTful, CRUD web application that stores places of interest in a database and displays them either using a grid or a list layout back to the user.

The user can add, edit or delete places from the database. It uses the [Play framework](https://www.playframework.com/) and the MVC pattern, and is currently configured to run locally on one's machine.

There is also a Java implementation of this project [here](https://github.com/muhsinali/picture-gallery-java) (rewrote it in Scala so I could learn Scala :grinning:).

### How to run the web app
To run the web app locally, start the MongoDB database using `mongod` and then go to the root directory of this project and run `activator run`. Once ready, go to [http://localhost:9000](http://localhost:9000) (if running for the first time, will need to wait a bit for the source code to compile).

### Tech stack
- Scala 2.11.8
- Play framework (version 2.5.12)
- MongoDB (ReactiveMongo 0.12.1)
- HTML, CSS, Bootstrap 3, [Twirl template engine](https://www.playframework.com/documentation/2.5.x/ScalaTemplates)




Roadmap
=================================
- [X] Use GitFlow and set up a continuous deployment pipeline using Travis and Heroku
- [ ] Optimise page load speeds:
    - [X] Store static assets using Amazon S3
    - [X] Use [Scrimage](https://github.com/sksamuel/scrimage) to dynamically generate thumbnails for every uploaded image (to reduce the size of the images used in the grid and list views)
    - [X] Use Scrimage to ensure all uploaded images are progressive JPEGs; improves perceived performance
    - [X] Use Amazon CloudFront so that static assets are served from the closest edge location; reduces the latency of delivering images
    - [X] Look for performance improvements using WebPageTest ([performance results](https://www.webpagetest.org/result/170331_19_6DGB/5/performance_optimization/)) and implement them.
    - [ ] Add pagination to limit the number of images shown on the page
- [ ] Add user authentication




Known issues
=================================

1. Would like to ensure that the user has uploaded a picture (i.e. I would like this to be a requirement using form validation).

2. Use Place objects in the routes file instead of Int, but not sure how to uniquely represent a Place object in a URL without using a GUID. GUIDs aren't a good idea as they make the URL harder to read and also expose the ID of Place objects stored in the database to the user. This is a security vulnerability. Would prefer a self-descriptive, readable URL such as:
    https://www.example.com/this-is-a-self-descriptive-url

3. Would like to improve the tests that I have currently written. Not sure how to include a mock object in a HTTP request that's then sent to the web app.

4. Getting flash messages to show up on some redirect requests, as they provide useful information to the user.


If you have any suggestions, please submit a pull request/issue/bug. Always looking for feedback! 