package org.eu.nl.laszlo.rfm

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Cancellable, PoisonPill}
import akka.event.Logging
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import org.eu.nl.laszlo.rfm.Protocol.NewState
import org.eu.nl.laszlo.rfm.actor.{ClientRegistryActor, FridgeActor}
import upickle.default._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait FridgeWebSocketRequestHandler extends UpickleSupport with Directives {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def executionContext: ExecutionContext

  implicit def materializer: Materializer

  lazy val log = Logging(system, classOf[FridgeWebSocketRequestHandler])

  def route: Route = {

    val (outActor, fridgeBroadcast): (ActorRef, Source[TextMessage, NotUsed]) =
      Source.actorRef[Protocol.Response](256, OverflowStrategy.dropTail)
        .map(response => TextMessage(write(response))) //TODO: to be able to conflate efficiently, we need to serialize later.
        .toMat(BroadcastHub.sink(1))(Keep.both)
        .run()

    val clientRegistryActor: ActorRef = system.actorOf(ClientRegistryActor.props(outActor), name = "client-registry")

    val fridgeActor: ActorRef = system.actorOf(FridgeActor.props(clientRegistryActor), "fridge")

    val fanIn = Sink.actorRef(fridgeActor, PoisonPill)
      .runWith(
        MergeHub.source[Message]
          .mapAsync(1) {
            case tm: TextMessage => tm.toStrict(10.seconds).map(t => read[Protocol.Request](t.text))
            case bm: BinaryMessage =>
              // consume the stream
              bm.dataStream.runWith(Sink.ignore)
              Future.failed(new Exception("yuck"))
          }
      )

    val ticker: Source[Int, Cancellable] = Source.tick(1.second, 1.second, 1).scan(0)(_ + _)

    val clientTicker = ticker
      .map(_ => TextMessage(write(NewState(Map.empty, partial = true))))

    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        // Scala-JS puts them in the root of the resource directory per default,
        // so that's where we pick them up
        path("frontend-fastopt-bundle.js")(getFromResource("frontend-fastopt-bundle.js")) ~
        // the websocket
        path("rfm") {
          handleWebSocketMessages(Flow.fromSinkAndSource(fanIn, fridgeBroadcast.merge(clientTicker, eagerComplete = true)))
        }
    } ~ getFromResourceDirectory("web")
  }
}
