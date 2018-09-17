package org.eu.nl.laszlo.rfm

import java.util.UUID

//model
object Magnet {
  def apply(text: CharSequence): Magnet = {
    val uuid = UUID.randomUUID()
    Magnet((uuid.getMostSignificantBits, uuid.getLeastSignificantBits), text.toString)
  }
}

final case class Magnet(handle: (Long, Long), text: String)

final case class Point(x: Int, y: Int)

//requests
final case object GetFullState

final case class GrabMagnet(magnet: Magnet)

final case class DragMagnet(magnet: Magnet, toPoint: Point)

final case class ReleaseMagnet(magnet: Magnet)

//responses
final case class NewState(positions: Map[Magnet, Point], partial: Boolean)

final case class MagnetGrabbed(magnet: Magnet, grabber: String)

final case class MagnetReleased(magnet: Magnet)