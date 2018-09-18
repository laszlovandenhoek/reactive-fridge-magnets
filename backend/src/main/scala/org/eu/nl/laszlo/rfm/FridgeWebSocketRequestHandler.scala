package org.eu.nl.laszlo.rfm

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.event.Logging
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

trait FridgeWebSocketRequestHandler extends JsonSupport with Directives {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def materializer: Materializer

  lazy val log = Logging(system, classOf[FridgeWebSocketRequestHandler])

  // other dependencies that UserRoutes use
  def clientRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  private val ticker: Source[Int, Cancellable] = Source.tick(1.second, 1.second, 1).scan(0)(_ + _)

  lazy val fridgeBroadcast: Source[TextMessage.Strict, NotUsed] = ticker
    .map(tick => TextMessage(s"Server tick $tick"))
    .runWith(BroadcastHub.sink(1))

  private val clientTicker = ticker
    .map(tick => TextMessage(s"Client tick $tick"))

  def route =
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        // Scala-JS puts them in the root of the resource directory per default,
        // so that's where we pick them up
        path("frontend-launcher.js")(getFromResource("frontend-launcher.js")) ~
        path("frontend-fastopt.js")(getFromResource("frontend-fastopt.js")) ~
        path("frontend-fastopt-bundle.js")(getFromResource("frontend-fastopt-bundle.js")) ~
        path("rfm") {
          handleWebSocketMessages(Flow.fromSinkAndSource(Sink.foreach(println), fridgeBroadcast.merge(clientTicker, eagerComplete = true)))
        }
    } ~ getFromResourceDirectory("web")

}
