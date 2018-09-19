package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object ClientRegistryActor {
  def props(broadcast: ActorRef): Props = Props(new ClientRegistryActor(broadcast))
}

class ClientRegistryActor(broadcast: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg => broadcast ! msg
  }
}
