package org.eu.nl.laszlo.rfm

import java.util.UUID

import org.scalajs.dom._
import upickle.default._

object Main {
  def main(args: Array[String]): Unit = {
    joinChat(UUID.randomUUID().toString)
  }

  def joinChat(name: String): Unit = {
    val playground = document.getElementById("playground")
    playground.innerHTML = s"Trying to join chat as '$name'..."
    val chat = new WebSocket(getWebsocketUri(name))
    chat.onopen = { event: Event =>
      playground.insertBefore(p("Chat connection was successful!"), playground.firstChild)

      //TODO: put event handlers on all magnets

      event
    }
    chat.onerror = { event: Event =>
      playground.insertBefore(p(s"Failed: code: ${event.asInstanceOf[ErrorEvent].colno}"), playground.firstChild)

      //TODO: retry connection

    }
    chat.onmessage = { event: MessageEvent =>
      val wsMsg = read[Protocol.Response](event.data.toString)

      wsMsg match {
        case Protocol.NewState(positions, partial) =>
          writeToArea(s"${if (partial) "updated positions" else "new positions"}: $positions")
        case Protocol.MagnetGrabbed(magnet, grabber) =>
          writeToArea(s"$magnet grabbed by $grabber")
        case Protocol.MagnetReleased(magnet) =>
          writeToArea(s"$magnet released")
      }
    }
    chat.onclose = { event: Event =>
      playground.insertBefore(p("Connection to chat lost. You can try to rejoin manually."), playground.firstChild)
    }

    def writeToArea(text: String): Unit =
      playground.innerHTML = text
  }

  def p(msg: String): Element = {
    val paragraph = document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }

  def getWebsocketUri(participant: String): String = {
    val wsProtocol = if (document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${document.location.host}/rfm?name=$participant"
  }

}
