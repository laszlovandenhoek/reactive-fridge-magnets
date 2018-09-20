package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.eu.nl.laszlo.rfm.Protocol.{GetFullState, InternalProtocol, Request, Response}
import org.eu.nl.laszlo.rfm.actor.ClientRegistryActor.ClientConnected

object ClientRegistryActor {
  def props(broadcast: ActorRef): Props = Props(new ClientRegistryActor(broadcast))

  case class ClientConnected(name: String, out: SourceQueueWithComplete[Response]) extends InternalProtocol

}

class ClientRegistryActor(broadcast: ActorRef) extends Actor with ActorLogging {

  val fridge: ActorRef = context.actorOf(FridgeActor.props(self), "fridge")

  var clients: Set[ActorRef] = Set.empty

  override def receive: Receive = {
    case ClientConnected(name, out) =>
      val newClient = context.actorOf(ConnectedClientActor.props(out), name)
      clients += newClient
      fridge.tell(GetFullState, newClient)
    case request: Request => fridge ! request
    case response: Response => broadcast ! response
  }
}
