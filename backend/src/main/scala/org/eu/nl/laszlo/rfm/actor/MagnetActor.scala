package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import org.eu.nl.laszlo.rfm.Protocol._

import scala.concurrent.duration._

object MagnetActor {

  def props(text: String, canvas: Square, broadcast: ActorRef): Props = Props(new MagnetActor(text, canvas, broadcast))
}

class MagnetActor(text: String, canvas: Square, broadcast: ActorRef) extends Actor with ActorLogging {

  def magnet: Magnet = Magnet(self.path.name, text)

  override def receive: Receive = available(canvas.randomPointWithin())

  def available(position: Point): Receive = {
    case Locate =>
      sender() ! NewPositions(Map(magnet -> position), partial = true)
    case Grab =>
      broadcast ! MagnetGrabbed(magnet, sender().path.name)
      context.become(grabbed(
        position,
        sender(),
        context.system.scheduler.scheduleOnce(20.seconds, self, Release)(context.dispatcher, sender())
      ))
    case Drag =>
      log.debug("illegal drag by {} (not grabbed)", sender())
  }

  def grabbed(position: Point, grabber: ActorRef, releaseTimeout: Cancellable): Receive = {
    case Locate =>
      sender() ! NewPositions(Map(magnet -> position), partial = true)
      sender() ! MagnetGrabbed(magnet, grabber.path.name)
    case Release if sender() == grabber =>
      releaseTimeout.cancel()
      broadcast ! MagnetReleased(magnet)
      context.become(available(position))
    case Drag(to) if sender() == grabber && canvas.contains(to) =>
      broadcast ! NewPositions(Map(magnet -> to), partial = true)
      context.become(grabbed(to, grabber, releaseTimeout))
    case Drag(outOfBounds) if !canvas.contains(outOfBounds) =>
      log.debug("illegal drag by {} ({} is out of bounds)", sender(), outOfBounds)
    case _: Drag if sender() != grabber =>
      log.debug("illegal drag by {} (grabber is {})", sender(), grabber.path.name)
  }

}
