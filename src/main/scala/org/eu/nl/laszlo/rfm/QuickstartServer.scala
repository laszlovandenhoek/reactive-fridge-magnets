package org.eu.nl.laszlo.rfm

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.eu.nl.laszlo.rfm.actor.{ClientRegistryActor, FridgeActor}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object QuickstartServer extends App with FridgeWebSocketRequestHandler {

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("akka-http-backend")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val clientRegistryActor: ActorRef = system.actorOf(ClientRegistryActor.props)

  val sessionsActor: ActorRef = system.actorOf(FridgeActor.props(clientRegistryActor), "sessions-actor")

  private val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandleSync(handleRequest, "0.0.0.0", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      counter //start the ~~fridge~~ counter
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
