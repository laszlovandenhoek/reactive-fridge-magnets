package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, Props}

object ClientRegistryActor {
  def props: Props = Props[ClientRegistryActor]
}

class ClientRegistryActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case _ =>
  }
}