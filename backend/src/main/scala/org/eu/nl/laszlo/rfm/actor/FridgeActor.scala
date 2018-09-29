package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.eu.nl.laszlo.rfm.Protocol._

import scala.collection.Set
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object FridgeActor {

  private final val charFrequencies: Map[String, Int] = Map(
    "a" -> 8,
    "b" -> 1,
    "c" -> 1,
    "d" -> 5,
    "e" -> 19,
    "f" -> 1,
    "g" -> 3,
    "h" -> 3,
    "i" -> 6,
    "j" -> 2,
    "k" -> 3,
    "l" -> 4,
    "m" -> 3,
    "n" -> 10,
    "o" -> 6,
    "p" -> 1,
    "q" -> 1,
    "r" -> 6,
    "s" -> 4,
    "t" -> 6,
    "u" -> 2,
    "v" -> 2,
    "w" -> 2,
    "x" -> 1,
    "y" -> 1,
    "z" -> 1
  )

  private final val canvas: Square = Square(Point.origin, Point(1280, 720))

  private def createMagnets(): Set[Magnet] = {
    for {
      (char, freq) <- charFrequencies.toSet
      id <- 1 to freq
    } yield {
      Magnet.apply(id, char.toString)
    }
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

    context.system.scheduler.schedule(4.seconds, 30.seconds) {
      randomItemFromSet(magnets).foreach { pickedMagnet =>
        self ! GrabMagnet(pickedMagnet)
        context.system.scheduler.scheduleOnce(1.second, self, DragMagnet(pickedMagnet, canvas.randomPointWithin()))
        context.system.scheduler.scheduleOnce(2.seconds, self, ReleaseMagnet)
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
      sender() ! AggregateStateChange(
        moved = Some(NewPositions(positions, partial = false)),
        grabbed = draggers.map({ case (a, b) => MagnetGrabbed(b, a) }).toSet)
    case GrabMagnet(magnet) =>
      val client = sender()

      log.info("{} asked to grab {}", actorRefToString(client), magnet)

      val clientDraggingMagnet = draggers.find({ case (_, m) => m == magnet }).map(_._1)
      val draggedByClient = draggers.get(client)

      (clientDraggingMagnet, draggedByClient) match {
        case (None, currentDrag) => //client is not dragging anything
          context.become(receive(draggers.updated(client, magnet), positions))
          broadcast(MagnetGrabbed(magnet, client))
          currentDrag.foreach(some => {
            log.warning("{} was already dragging {}; switching to {}", client, some, magnet)
            broadcast(MagnetReleased(some))
          })
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