package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.eu.nl.laszlo.rfm.Protocol._
import org.eu.nl.laszlo.rfm.actor.ChaosMonkey.ClientList
import org.eu.nl.laszlo.rfm.actor.ClientRegistryActor.ClientConnected

object ClientRegistryActor {
  def props(broadcast: ActorRef, canvas: Square): Props = Props(new ClientRegistryActor(broadcast, canvas))

  case class ClientConnected(name: String, out: SourceQueueWithComplete[Response]) extends InternalRequest

}

class ClientRegistryActor(broadcast: ActorRef, canvas: Square) extends Actor with ActorLogging {

  private val fridge: ActorRef = context.actorOf(FridgeActor.props(self, canvas), "fridge")

  private val monkey: ActorRef = context.actorOf(ChaosMonkey.props(canvas), ChaosMonkey.name)

  override def receive: Receive = receive(Set.empty)

  def receive(clients: Set[ActorRef]): Receive = {
    case ClientConnected(name, out) =>
      log.info("client connected: {}", name)
      val newClient: ActorRef = context.actorOf(ConnectedClientActor.props(out), name)
      context.watch(newClient)
      val newClients = clients + newClient
      context.become(receive(newClients))
      fridge.tell(GetFullState, newClient)
      monkey ! ClientList(newClients)
    case Terminated(client) =>
      log.info("client terminated: {}", client.path.name)
      val newClients = clients - client
      context.become(receive(newClients))
      monkey ! ClientList(newClients)
    case ExternalRequestWrapper(name, request) =>
//      log.debug("received request {} from {}", request, name)
      context.child(name).foreach(c => fridge.tell(request, c))
    case response: Response =>
      broadcast ! response
      monkey ! response
  }
}
