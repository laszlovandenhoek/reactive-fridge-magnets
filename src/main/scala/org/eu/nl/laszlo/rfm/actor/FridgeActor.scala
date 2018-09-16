package org.eu.nl.laszlo.rfm.actor

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object FridgeActor {

  object Magnet {
    def apply(text: CharSequence): Magnet = {
      val uuid = UUID.randomUUID()
      Magnet((uuid.getMostSignificantBits, uuid.getLeastSignificantBits), text.toString)
    }
  }

  final case class Magnet(handle: (Long,Long), text: String)

  final case class Point(x: Int, y: Int)

  final val canvasSizeHorizontal: Int = 1000
  final val canvasSizeVertical: Int = 1000

  private def createMagnets(): Set[Magnet] = {
    Set('a' to 'z').map(Magnet.apply(_))
  }

  private def withinBounds(point: Point): Boolean =
    point.x >= 0 && point.x < canvasSizeHorizontal &&
      point.y >= 0 && point.y < canvasSizeVertical

  def props(clientRegistry: ActorRef): Props = Props[FridgeActor](new FridgeActor(clientRegistry))

  //requests
  final case object GetFullState

  final case class GrabMagnet(magnet: Magnet)

  final case class DragMagnet(magnet: Magnet, toPoint: Point)

  final case class ReleaseMagnet(magnet: Magnet)

  //responses
  final case class NewState(positions: Map[Magnet, Point], partial: Boolean)

  final case class MagnetGrabbed(magnet: Magnet, grabber: String) //TODO: this could be a typed ActorRef, I think

  final case class MagnetReleased(magnet: Magnet)

}

class FridgeActor(clientRegistry: ActorRef) extends Actor with ActorLogging {

  import FridgeActor._

  val magnets: Set[Magnet] = createMagnets()
  var draggers: Map[String, Magnet] = Map.empty
  var positions: Map[Magnet, Point] = magnets.map(m => (m, Point(0, 0))).toMap

  //not sure how clients will be represented so keeping this one around for now
  implicit private def actorRefToString(actorRef: ActorRef): String = actorRef.path.name

  def receive: Receive = {
    case GetFullState =>
      sender() ! NewState(positions, partial = false)
    case GrabMagnet(magnet) =>
      val client = sender()
      val clientDraggingMagnet = draggers.find({ case (_, m) => m == magnet }).map(_._1)
      val draggedByClient = draggers.get(client)

      (clientDraggingMagnet, draggedByClient) match {
        case (None, None) => //client is not dragging anything, and this magnet is free
          setDragger(client, magnet)
        case (None, Some(otherMagnet)) => //client is already dragging a different magnet, but this magnet is free
          log.warning("{} was already dragging {}; switching to {}", client, otherMagnet, magnet)
          setDragger(client, magnet)
        case (Some(dragger), Some(`magnet`)) if dragger == actorRefToString(client) => //client is already dragging this magnet
          client ! MagnetGrabbed(magnet, client) //no need for a separate case object AlreadyDragging, just a friendly reminder
        case (Some(otherClient), _) => //some other bastard got it first :(
          client ! MagnetGrabbed(magnet, otherClient)
      }
    case DragMagnet(magnet, toPoint) =>
      if (draggers.get(sender()).contains(magnet) && withinBounds(toPoint)) {
        positions = positions.updated(magnet, toPoint)
        broadcast(NewState(positions, partial = true))
      }
    case ReleaseMagnet(magnet) =>
      val client = sender()
      if (draggers.get(client).contains(magnet))
        draggers = draggers - client

  }

  private def setDragger(connectedClient: ActorRef, magnet: Magnet): Unit = {
    draggers = draggers.updated(connectedClient, magnet)
    broadcast(MagnetGrabbed(magnet, connectedClient))
  }

  private def broadcast(message: Any): Unit = {
    clientRegistry ! message
  }
}