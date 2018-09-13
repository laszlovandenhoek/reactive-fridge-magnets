package org.eu.nl.laszlo

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

//this one should move to ClientRegistry once it's created
final case class ConnectedClient(handle: UUID, nickname: String)

final case class Magnet(handle: UUID, text: String)

final case class Point(x: Int, y: Int)

object FridgeActor {

  //requests
  final case object GetFullState

  final case class GrabMagnet(magnet: Magnet, grabber: ConnectedClient)

  final case class DragMagnet(magnet: Magnet, toPoint: Point)

  final case class ReleaseMagnet(magnet: Magnet) //check if ConnectedClient == sender() ?

  //responses
  final case class NewState(magnets: Set[Magnet], partial: Boolean)

  final case class MagnetGrabbed(magnet: Magnet, grabber: ConnectedClient)

  final case class MagnetsMoved(magnets: Set[Magnet])

  final case class MagnetReleased(magnet: Magnet)

  def props(clientRegistry: ActorRef) = Props[FridgeActor](new FridgeActor(clientRegistry))
}

class FridgeActor(clientRegistry: ActorRef) extends Actor with ActorLogging {

  import FridgeActor._

  val magnets: Set[Magnet] = createMagnets()
  var draggedBy: Map[Magnet, ConnectedClient] = Map.empty
  var positions: Map[Magnet, Point] = magnets.map(m => (m, Point(0, 0))).toMap

  def receive: Receive = {
    case GetFullState =>
      sender() ! NewState(magnets, false)
    case GrabMagnet(magnet, hopeful) =>
      draggedBy.get(magnet) match {
        case None =>
          broadcast(MagnetGrabbed(magnet, hopeful))
        case Some(`hopeful`) =>
          sender() ! MagnetGrabbed(magnet, hopeful)
        case Some(otherBastard) =>
          sender() ! MagnetGrabbed(magnet, otherBastard)
      }
    case DragMagnet(magnet, toPoint) =>
      ???
    case ReleaseMagnet(name) =>
      ???
  }

  def broadcast(message: Any): Unit = {
    clientRegistry ! message
  }

  private def createMagnets(): Set[Magnet] = {
    Set.empty
  }

}