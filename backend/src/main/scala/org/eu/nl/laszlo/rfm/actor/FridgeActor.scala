package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.eu.nl.laszlo.rfm.Protocol._

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

  def props(clientRegistry: ActorRef): Props = Props[FridgeActor](new FridgeActor(clientRegistry))

}

class FridgeActor(clientRegistry: ActorRef) extends Actor with ActorLogging {

  import FridgeActor._


  private def randomItem[T](it: Iterable[T]): Option[T] = {
    val i = Random.nextInt(it.size)
    it.view(i, i + 1).headOption
  }

  override def preStart(): Unit = {

    implicit val dispatcher: ExecutionContext = context.dispatcher

    for {
      (char, freq) <- charFrequencies.toSet
      id <- 1 to freq
    } context.actorOf(MagnetActor.props(char, canvas, clientRegistry), char + id)

    context.system.scheduler.schedule(500.milliseconds, 10.second) {
      self.tell(GetFullState, clientRegistry)
    }

    context.system.scheduler.schedule(4.seconds, 30.seconds) {
      randomItem(context.children).foreach { pickedMagnet =>
        pickedMagnet ! Grab
        context.system.scheduler.scheduleOnce(1.second, pickedMagnet, Drag(canvas.randomPointWithin()))
        context.system.scheduler.scheduleOnce(2.seconds, pickedMagnet, Release)
      }
    }

  }

  def receive: Receive = {
    case GetFullState =>
      context.children.foreach(_.tell(Locate, sender()))
    case mc: MagnetCommand =>
      context.child(mc.magnetHandle).foreach(_.tell(mc.message, sender()))
  }

}