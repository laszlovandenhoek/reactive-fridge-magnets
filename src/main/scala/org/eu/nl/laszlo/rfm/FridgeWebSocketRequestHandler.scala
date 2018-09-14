package org.eu.nl.laszlo.rfm

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.event.Logging.InfoLevel
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, UpgradeToWebSocket}
import akka.stream.Attributes.logLevels
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

trait FridgeWebSocketRequestHandler extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def materializer: Materializer

  lazy val log = Logging(system, classOf[FridgeWebSocketRequestHandler])

  // other dependencies that UserRoutes use
  def sessionsActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val (cancel, counter) =
    Source.tick(0.seconds, 1.second, 1).scan(0)(_ + _)
      .log("tick").withAttributes(logLevels(onElement = InfoLevel))
      .map(tick => TextMessage(s"Tick $tick"))
      .buffer(1, OverflowStrategy.dropBuffer)
      .preMaterialize()

  def handleRequest(request: HttpRequest): HttpResponse = request match {
    case req@HttpRequest(GET, Uri.Path("/rfm"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) =>
          upgrade.handleMessagesWithSinkSource(Sink.foreach(println), counter)
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

}
