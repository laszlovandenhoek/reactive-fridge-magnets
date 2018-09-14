package org.eu.nl.laszlo.rfm.actor

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}

object ClientRegistryActor {
  //this should be an Actor of its own
  final case class ConnectedClient(handle: UUID, nickname: String)

  def props: Props = Props[ClientRegistryActor]
}

class ClientRegistryActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case _ =>
  }
}
