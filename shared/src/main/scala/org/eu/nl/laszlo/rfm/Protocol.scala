package org.eu.nl.laszlo.rfm

import java.util.UUID

import upickle.default.{ReadWriter => RW, macroRW}

object Protocol {

  //model
  object Magnet {
    def apply(text: CharSequence): Magnet = {
      val uuid = UUID.randomUUID()
      Magnet((uuid.getMostSignificantBits, uuid.getLeastSignificantBits), text.toString)
    }

    implicit def rw: RW[Magnet] = macroRW
  }

  final case class Magnet(handle: (Long, Long), text: String)

  object Point {
    implicit def rw: RW[Point] = macroRW
  }

  final case class Point(x: Int, y: Int)

  //requests
  object Request {
    implicit def rw: RW[Request] = RW.merge(GetFullState.rw, GrabMagnet.rw, DragMagnet.rw, ReleaseMagnet.rw)
  }

  sealed trait Request

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

  object ReleaseMagnet {
    implicit def rw: RW[ReleaseMagnet] = macroRW
  }

  final case class ReleaseMagnet(magnet: Magnet) extends Request


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