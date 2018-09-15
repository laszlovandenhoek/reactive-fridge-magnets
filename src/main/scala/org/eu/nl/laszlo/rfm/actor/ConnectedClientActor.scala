package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, Props}
import java.util.UUID

object ConnectedClientActor {

  //TODO: it would be easier if this were a chield of ClientRegistryActor

  def props: Props = Props[ConnectedClientActor](new ConnectedClientActor(UUID.randomUUID()))
}

class ConnectedClientActor(uuid: UUID) extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ =>
  }
}
