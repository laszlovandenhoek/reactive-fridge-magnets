package org.eu.nl.laszlo.rfm

import upickle.default.{macroRW, ReadWriter => RW}

import scala.util.Random

object Protocol {

  //model
  object Magnet {
    def apply(id: Int, text: CharSequence): Magnet = {
      Magnet(text.toString + id, text.toString)
    }

    implicit def rw: RW[Magnet] = macroRW
  }

  final case class Magnet(handle: String, text: String)

  object Point {
    val origin = Point(0, 0)

    implicit def rw: RW[Point] = macroRW

    def randomWithin(bounds: Square): Point = bounds.randomPointWithin()

  }

  final case class Square(origin: Point, farthest: Point) {
    private val xRange = origin.x to farthest.x
    private val yRange = origin.y to farthest.y

    def contains(p: Point): Boolean = {
      xRange.contains(p.x) && yRange.contains(p.y)
    }

    def randomPointWithin(): Point = {
      Point(
        Random.nextInt(farthest.x - origin.x),
        Random.nextInt(farthest.y - origin.y)
      )
    }

  }

  final case class Point(x: Int, y: Int) {
    def isInSquare(s: Square): Boolean = s.contains(this)
  }

  //requests
  object Request {
    implicit def rw: RW[Request] = RW.merge(GetFullState.rw, GrabMagnet.rw, DragMagnet.rw, ReleaseMagnet.rw)
  }

  trait InternalProtocol

  sealed trait Request extends InternalProtocol

  case object GetFullState extends Request {
    implicit def rw: RW[GetFullState.type] = macroRW
  }

  object GrabMagnet {
    implicit def rw: RW[GrabMagnet] = macroRW
  }

  final case class GrabMagnet(magnet: Magnet) extends Request

  object DragMagnet {
    implicit def rw: RW[DragMagnet] = macroRW
  }

  final case class DragMagnet(magnet: Magnet, toPoint: Point) extends Request

  case object ReleaseMagnet extends Request {
    implicit def rw: RW[ReleaseMagnet.type] = macroRW
  }


  //responses
  object Response {
    implicit def rw: RW[Response] = RW.merge(NewState.rw, MagnetGrabbed.rw, MagnetReleased.rw)
  }

  sealed trait Response

  object NewState {
    implicit def rw: RW[NewState] = macroRW
  }

  final case class NewState(positions: Map[Magnet, Point], partial: Boolean) extends Response

  object MagnetGrabbed {
    implicit def rw: RW[MagnetGrabbed] = macroRW
  }

  final case class MagnetGrabbed(magnet: Magnet, grabber: String) extends Response

  object MagnetReleased {

    implicit def rw: RW[MagnetReleased] = macroRW
  }

  final case class MagnetReleased(magnet: Magnet) extends Response

}