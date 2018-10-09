package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import org.eu.nl.laszlo.rfm.Protocol._
import org.eu.nl.laszlo.rfm.actor.ChaosMonkey.ClientList

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object ChaosMonkey {
  final val name: String = "ff0000"

  def props(canvas: Square): Props = Props(new ChaosMonkey(canvas))

  case class ClientList(clients: Set[ActorRef])

}

class ChaosMonkey(canvas: Square) extends Actor with ActorLogging {

  private var chaos: Cancellable = Cancellable.alreadyCancelled
  private var state: AggregateStateChange = AggregateStateChange()

  def randomItem[T](it: Iterable[T]): Option[T] = {
    val i = Random.nextInt(it.size)
    it.view(i, i + 1).headOption
  }

  private def wrap(request: Request) = request.toExternalRequestWrapper(ChaosMonkey.name)

  def startChaos(): Unit = {
    log.info("it's chaos!")
    implicit val dispatcher: ExecutionContext = context.dispatcher
    chaos = context.system.scheduler.schedule(4.seconds, 4.seconds) {
      state.moved.map(_.positions.keys).flatMap(randomItem).map(_.handle).foreach { pickedMagnet =>
        context.parent ! wrap(GrabMagnet(pickedMagnet))
        context.system.scheduler.scheduleOnce(1.second, context.parent, wrap(DragMagnet(pickedMagnet, canvas.randomPointWithin())))
        context.system.scheduler.scheduleOnce(2.seconds, context.parent, wrap(ReleaseMagnet(pickedMagnet)))
      }
    }
  }

  def cancelChaos(): Unit = {
    log.info("order has been restored")
    chaos.cancel()
  }

  override def preStart(): Unit = {
    context.parent ! ExternalRequestWrapper(ChaosMonkey.name, GetFullState)
  }

  override def receive: Receive = {
    case ClientList(clients) if clients.size == 1 =>
      startChaos()
    case _: ClientList =>
      cancelChaos()
    case r: Response => state = state.add(r)
  }

}
