package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.eu.nl.laszlo.rfm.Protocol._

import scala.concurrent.duration._

object FridgeActor {

  private final val canvas: Square = Square(Point.origin, Point(1000, 1000))

  private def createMagnets(): Set[Magnet] = {
    (for {
      char <- 'a' to 'z'
    } yield {
      Magnet.apply(1, char.toString)
    }).toSet
  }

  def props(clientRegistry: ActorRef): Props = Props[FridgeActor](new FridgeActor(clientRegistry))

}

class FridgeActor(clientRegistry: ActorRef) extends Actor with ActorLogging {

  import FridgeActor._

  private val magnets: Set[Magnet] = createMagnets()

  override def preStart(): Unit = {
    context.system.scheduler.schedule(1.second, 1.second) {
      self.tell(GetFullState, clientRegistry)
    }(context.dispatcher)

    context.system.scheduler.schedule(1.second, 1.second) {
      val pickedMagnet = magnets.head //TODO: more random
      self ! GrabMagnet(pickedMagnet)
      self ! DragMagnet(pickedMagnet, canvas.randomPointWithin())
      self ! ReleaseMagnet
    }(context.dispatcher)

  }

  import scala.language.implicitConversions
  implicit private def actorRefToString(actorRef: ActorRef): String = actorRef.path.name

  def receive: Receive = receive(
    draggers = Map.empty,
    positions = magnets.map(m => (m, canvas.randomPointWithin())).toMap
  )

  def receive(draggers: Map[String, Magnet], positions: Map[Magnet, Point]): Receive = {
    case GetFullState =>
      sender() ! NewState(positions, partial = false)
    case GrabMagnet(magnet) =>
      val client = sender()
      val clientDraggingMagnet = draggers.find({ case (_, m) => m == magnet }).map(_._1)
      val draggedByClient = draggers.get(client)

      (clientDraggingMagnet, draggedByClient) match {
        case (None, currentDrag) => //client is not dragging anything
          context.become(receive(draggers.updated(client, magnet), positions))
          broadcast(MagnetGrabbed(magnet, client))
          currentDrag.foreach(some => log.warning("{} was already dragging {}; switching to {}", client, some, magnet))
        case (Some(dragger), Some(`magnet`)) if dragger == actorRefToString(client) => //client is already dragging this magnet
          client ! MagnetGrabbed(magnet, client) //no need for a separate case object AlreadyDragging, just a friendly reminder
        case (Some(otherClient), _) => //some other bastard got it first :(
          client ! MagnetGrabbed(magnet, otherClient)
      }
    case DragMagnet(magnet, toPoint) =>
      if (draggers.get(sender()).contains(magnet) && canvas.contains(toPoint)) {
        context.become(receive(draggers, positions + (magnet -> toPoint)))
        broadcast(NewState(Map(magnet -> toPoint), partial = true))
      }
    case ReleaseMagnet =>
      draggers.get(sender()).foreach(releasedMagnet => broadcast(MagnetReleased.apply(releasedMagnet)))
      context.become(receive(draggers - sender(), positions))
  }


  private def broadcast(message: Any): Unit = {
    clientRegistry ! message
  }
}