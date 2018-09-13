package org.eu.nl.laszlo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  //
  //  implicit val userJsonFormat = jsonFormat3(User)
  //  implicit val usersJsonFormat = jsonFormat1(Users)
  //
  //  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
