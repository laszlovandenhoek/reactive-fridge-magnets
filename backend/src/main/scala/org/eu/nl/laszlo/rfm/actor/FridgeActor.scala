package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.eu.nl.laszlo.rfm.Protocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object FridgeActor {

  private final val canvas: Square = Square(Point.origin, Point(1280, 720))

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

  private def randomItemFromSet[T](set: Set[T]): Option[T] = {
    val i = Random.nextInt(set.size)
    set.view(i, i + 1).headOption
  }

  override def preStart(): Unit = {

    implicit val dispatcher: ExecutionContext = context.dispatcher

    context.system.scheduler.schedule(500.milliseconds, 10.second) {
      self.tell(GetFullState, clientRegistry)
    }

    context.system.scheduler.schedule(1.second, 500.milliseconds) {
      randomItemFromSet(magnets).foreach { pickedMagnet =>
        self ! GrabMagnet(pickedMagnet)
        context.system.scheduler.scheduleOnce(100.milliseconds, self, DragMagnet(pickedMagnet, canvas.randomPointWithin()))
        context.system.scheduler.scheduleOnce(200.milliseconds, self, ReleaseMagnet)
      }
    }

  }

  import scala.language.implicitConversions

  implicit private def actorRefToString(actorRef: ActorRef): String = actorRef.path.name

  def receive: Receive = receive(
    draggers = Map.empty,
    positions = magnets.map(m => (m, canvas.randomPointWithin())).toMap
  )

  def receive(draggers: Map[String, Magnet], positions: Map[Magnet, Point]): Receive = {
    case GetFullState =>
      sender() ! NewPositions(positions, partial = false)
      sender() ! AggregateStateChange(grabbed = draggers.map({ case (a, b) => MagnetGrabbed(b, a) }).toSet)
    case GrabMagnet(magnet) =>
      val client = sender()

      log.info("{} asked to grab {}", actorRefToString(client), magnet)

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
      val client = sender()

      log.info("{} asked to drag {} to {}", actorRefToString(client), magnet, toPoint)

      if (draggers.get(client).contains(magnet) && canvas.contains(toPoint)) {
        context.become(receive(draggers, positions + (magnet -> toPoint)))
        broadcast(NewPositions(Map(magnet -> toPoint), partial = true))
      }
    case ReleaseMagnet =>
      val client = sender()

      log.info("{} asked to release magnet", actorRefToString(client))

      draggers.get(client).foreach(releasedMagnet => broadcast(MagnetReleased.apply(releasedMagnet)))
      context.become(receive(draggers - client, positions))
  }


  private def broadcast(message: Any): Unit = {
    clientRegistry ! message
  }
}