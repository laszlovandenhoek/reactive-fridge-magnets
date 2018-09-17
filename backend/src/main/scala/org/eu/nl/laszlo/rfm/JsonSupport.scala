package org.eu.nl.laszlo.rfm

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsonFormat}

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val jfMagnet: JsonFormat[Magnet] = jsonFormat2(Magnet.apply)
  implicit val jfPoint: JsonFormat[Point] = jsonFormat2(Point)

  implicit val jfNewState: JsonFormat[NewState] = jsonFormat2(NewState)
  implicit val jfMagnetGrabbed: JsonFormat[MagnetGrabbed] = jsonFormat2(MagnetGrabbed)
  implicit val jfMagnetReleased: JsonFormat[MagnetReleased] = jsonFormat1(MagnetReleased)

}
