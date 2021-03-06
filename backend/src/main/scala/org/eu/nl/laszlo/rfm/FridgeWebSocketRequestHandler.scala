package org.eu.nl.laszlo.rfm

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Concat, Flow, GraphDSL, Keep, MergeHub, Sink, Source, SourceQueueWithComplete}
import akka.{Done, NotUsed}
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import org.eu.nl.laszlo.rfm.Protocol.{ExternalRequestWrapper, Point, Response, Square}
import org.eu.nl.laszlo.rfm.actor.ClientRegistryActor
import org.eu.nl.laszlo.rfm.actor.ClientRegistryActor.ClientConnected
import upickle.default._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait FridgeWebSocketRequestHandler extends UpickleSupport with Directives {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def executionContext: ExecutionContext

  private lazy final val canvas: Square = Square(Point.origin, Point(1280, 720))

  def route: Route = {

    val (outActor: ActorRef, fridgeBroadcast: Source[Response, NotUsed]) =
      Source.actorRef[Protocol.Response](128, OverflowStrategy.dropHead)
        .toMat(BroadcastHub.sink(1))(Keep.both)
        .run()

    //force demand so the buffer doesn't overrun
    fridgeBroadcast.to(Sink.ignore).run()

    val clientRegistryActor: ActorRef = system.actorOf(ClientRegistryActor.props(outActor, canvas), name = "client-registry")

    def wsToInternalProtocol(name: String)(message: Message): Future[Protocol.InternalRequest] = message match {
      case tm: TextMessage => tm.toStrict(10.seconds).map(t => read[Protocol.Request](t.text)).map(req => ExternalRequestWrapper(name, req))
      case bm: BinaryMessage =>
        // consume the stream
        bm.dataStream.runWith(Sink.ignore)
        Future.failed(new Exception("yuck"))
    }

    val fanIn: Sink[Protocol.InternalRequest, NotUsed] = MergeHub.source[Protocol.InternalRequest].to(Sink.actorRef(clientRegistryActor, PoisonPill)).run()

    def startWith[T](elem: T) = Flow.fromGraph(
      GraphDSL.create(Concat[T]()) { implicit builder =>
        concat =>
          import GraphDSL.Implicits._
          Source.single(elem) ~> concat.in(0)
          FlowShape.of(concat.in(1), concat.out)
      }
    )

    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        // Scala-JS puts them in the root of the resource directory per default,
        // so that's where we pick them up
        path("frontend-fastopt-bundle.js")(getFromResource("frontend-fastopt-bundle.js")) ~
        // the websocket
        path("rfm") {
          parameter(Symbol("name")) { name =>
            val (queue: SourceQueueWithComplete[Response], queueSource: Source[Response, NotUsed]) =
              Source.queue[Response](4096, OverflowStrategy.dropHead).preMaterialize()

            val (done: Future[Done], in: Sink[Message, NotUsed]) =
              Flow[Message].mapAsync(1)(wsToInternalProtocol(name))
                .via(startWith(ClientConnected(name, queue)))
                .watchTermination()(Keep.right)
                .to(fanIn)
                .preMaterialize()

            done.onComplete(_ => {
              queue.complete()
            })

            val out: Source[Message, NotUsed] =
              fridgeBroadcast.merge(queueSource, eagerComplete = true)
                .conflateWithSeed(_.asAggregate)(_ add _)
                .throttle(1, 50.milliseconds)
                .map(response => TextMessage(write(response)))

            handleWebSocketMessages(Flow.fromSinkAndSourceCoupled(in, out))

          }
        }
    } ~ getFromResourceDirectory("web")
  }
}
