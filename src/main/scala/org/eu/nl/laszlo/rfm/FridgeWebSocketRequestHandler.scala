package org.eu.nl.laszlo.rfm

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.event.Logging
import akka.event.Logging.InfoLevel
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, UpgradeToWebSocket}
import akka.stream.Attributes.logLevels
import akka.stream.scaladsl.{BroadcastHub, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import org.eu.nl.laszlo.rfm.actor.ConnectedClientActor

import scala.concurrent.duration._

trait FridgeWebSocketRequestHandler extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def materializer: Materializer

  lazy val log = Logging(system, classOf[FridgeWebSocketRequestHandler])

  // other dependencies that UserRoutes use
  def clientRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  private val ticker: Source[Int, Cancellable] = Source.tick(0.seconds, 1.second, 1).scan(0)(_ + _)

  lazy val fridgeBroadcast: Source[TextMessage.Strict, NotUsed] = ticker
    .map(tick => TextMessage(s"Server tick $tick"))
    .log("tick").withAttributes(logLevels(onElement = InfoLevel))
    .buffer(1, OverflowStrategy.dropHead)
    .runWith(BroadcastHub.sink)

  private val clientTicker = ticker
    .map(tick => TextMessage(s"Client tick $tick"))

  def handleRequest(request: HttpRequest): HttpResponse = request match {
    case req@HttpRequest(GET, Uri.Path("/rfm"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) =>
          val clientActor: ActorRef = system.actorOf(ConnectedClientActor.props)
          val actorSink = Sink.actorRefWithAck(clientActor, NotUsed, NotUsed, NotUsed)
          //TODO: actorSink properly implemented, use in place of Sink below. But how will we deser the input?
          upgrade.handleMessagesWithSinkSource(Sink.foreach(println), fridgeBroadcast.merge(clientTicker, eagerComplete = true))
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

}
