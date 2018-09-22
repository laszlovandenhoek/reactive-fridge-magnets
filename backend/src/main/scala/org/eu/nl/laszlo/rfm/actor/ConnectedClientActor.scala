package org.eu.nl.laszlo.rfm.actor

import java.util.UUID

import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Stash}
import akka.stream.QueueOfferResult
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.eu.nl.laszlo.rfm.Protocol.Response
import org.eu.nl.laszlo.rfm.actor.ConnectedClientActor.Ready

object ConnectedClientActor {
  def props(out: SourceQueueWithComplete[Response]): Props = Props[ConnectedClientActor](new ConnectedClientActor(UUID.randomUUID(), out))

  case object Ready

}

class ConnectedClientActor(uuid: UUID, out: SourceQueueWithComplete[Response]) extends Actor with ActorLogging with Stash {

  import akka.pattern.pipe
  import context.dispatcher

  override def receive: Receive = {
    case r: Response =>
      out.offer(r).map {
        case Enqueued => Ready
        case Dropped =>
          log.warning("message {} dropped", r)
          Ready
        case QueueClosed => PoisonPill
        case QueueOfferResult.Failure(e) =>
          log.error(e, "could not enqueue message")
          PoisonPill
      }.pipeTo(self)

      context.become(offered)

  }

  def offered: Receive = {
    case Ready =>
      unstashAll()
      context.become(receive)
    case _ => stash
  }

}
