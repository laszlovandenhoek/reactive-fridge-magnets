package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.eu.nl.laszlo.rfm.Protocol._
import org.eu.nl.laszlo.rfm.actor.ClientRegistryActor.ClientConnected

object ClientRegistryActor {
  def props(broadcast: ActorRef): Props = Props(new ClientRegistryActor(broadcast))

  case class ClientConnected(name: String, out: SourceQueueWithComplete[Response]) extends InternalRequest

}

class ClientRegistryActor(broadcast: ActorRef) extends Actor with ActorLogging {

  private val fridge: ActorRef = context.actorOf(FridgeActor.props(self), "fridge")

  override def receive: Receive = receive(Set.empty)

  def receive(clients: Set[ActorRef]): Receive = {
    case ClientConnected(name, out) =>
      val newClient: ActorRef = context.actorOf(ConnectedClientActor.props(out), name)
      context.become(receive(clients + newClient))
      fridge.tell(GetFullState, newClient)
    case ExternalRequestWrapper(name, request) =>
      context.child(name).foreach(c => fridge.tell(request, c))
    case response: Response => broadcast ! response
  }
}
