package org.eu.nl.laszlo.rfm.actor

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.stream.QueueOfferResult
import akka.stream.QueueOfferResult.{Dropped, Enqueued, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.eu.nl.laszlo.rfm.Protocol.{AggregateStateChange, Response}
import org.eu.nl.laszlo.rfm.actor.ConnectedClientActor.Ready

object ConnectedClientActor {
  def props(out: SourceQueueWithComplete[Response]): Props = Props[ConnectedClientActor](new ConnectedClientActor(out))

  case object Ready

}

class ConnectedClientActor(out: SourceQueueWithComplete[Response]) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  override def preStart(): Unit = {
    out.watchCompletion().onComplete(_ => {
      log.info("queue for {} completed", self.path.name)
      self ! PoisonPill
    })
  }

  override def receive: Receive = {
    case r: Response =>
      out.offer(r).map {
        case Enqueued => Ready
        case Dropped =>
          log.warning("message {} dropped", r)
          Ready
        case QueueClosed => PoisonPill
        case QueueOfferResult.Failure(e) =>
          log.error(e, "could not enqueue message {}", r)
          PoisonPill
      }.pipeTo(self)

      context.become(offered)

  }

  def offered: Receive = {
    case Ready =>
      context.become(receive)
    case r: Response =>
      context.become(queued(r.asAggregate))
    case x =>
      log.info(s"received unexpected $x")
  }

  def queued(aggregate: AggregateStateChange): Receive = {
    case Ready =>
      self ! aggregate
      context.become(receive)
    case r: Response =>
      context.become(queued(aggregate.add(r)))
    case x =>
      log.info(s"received unexpected $x")
  }

}
