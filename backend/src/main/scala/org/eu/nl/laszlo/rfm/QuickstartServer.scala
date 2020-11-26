package org.eu.nl.laszlo.rfm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object QuickstartServer extends App with FridgeWebSocketRequestHandler {

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("akka-http-backend")
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(route, "0.0.0.0", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://[${bound.localAddress.getHostString}]:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
