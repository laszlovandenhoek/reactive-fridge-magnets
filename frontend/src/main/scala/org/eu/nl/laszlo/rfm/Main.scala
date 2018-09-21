package org.eu.nl.laszlo.rfm

import java.util.UUID

import org.eu.nl.laszlo.rfm.Protocol.{Magnet, Point}
import org.scalajs.dom
import org.scalajs.dom._
import upickle.default._

object Main {

  val messages: html.Div = dom.document.getElementById("messages").asInstanceOf[html.Div]
  val canvas: html.Div = dom.document.getElementById("canvas").asInstanceOf[html.Div]

  def main(args: Array[String]): Unit = {
    joinChat(UUID.randomUUID().toString)
  }

  def joinChat(name: String): Unit = {
    messages.innerHTML = s"Trying to join chat as '$name'..."
    val chat = new WebSocket(getWebsocketUri(name))
    chat.onopen = { event: Event =>
      messages.insertBefore(p("Chat connection was successful!"), messages.firstChild)

      //TODO: put event handlers on all magnets

      event
    }
    chat.onerror = { event: Event =>
      messages.insertBefore(p(s"Failed: code: ${event.asInstanceOf[ErrorEvent].colno}"), messages.firstChild)

      //TODO: retry connection

    }
    chat.onmessage = { event: MessageEvent =>
      val wsMsg = read[Protocol.Response](event.data.toString)

      processResponse(wsMsg)
    }
    chat.onclose = { event: Event =>
      messages.insertBefore(p("Connection to chat lost. You can try to rejoin manually."), messages.firstChild)
    }

    def processResponse(r: Protocol.Response): Unit = {
      r match {
        case Protocol.AggregateStateChange(moved, grabbed, released) =>
          (grabbed ++ released ++ moved.toSet).foreach(processResponse)
        case Protocol.NewPositions(positions, partial) =>
          writeToArea(s"${if (partial) "updated positions" else "new positions"}: $positions")
          positions.foreach((updateMagnet _).tupled)
        case Protocol.MagnetGrabbed(magnet, grabber) =>
          writeToArea(s"$magnet grabbed by $grabber")
        case Protocol.MagnetReleased(magnet) =>
          writeToArea(s"$magnet released")
      }
    }

    def writeToArea(text: String): Unit =
      messages.innerHTML = text
  }

  def updateMagnet(magnet: Magnet, point: Point): Unit = {
    var magnetHTML = dom.document.getElementById(magnet.handle).asInstanceOf[html.Paragraph]
    if (magnetHTML == null) {
      magnetHTML = p(magnet.text)
      magnetHTML.id = magnet.handle
      magnetHTML.style.position = "absolute"
      canvas.appendChild(magnetHTML)
    }
    magnetHTML.style.left = point.x.toString
    magnetHTML.style.top = point.y.toString
  }

  def p(msg: String): html.Paragraph = {
    val paragraph = dom.document.createElement("p").asInstanceOf[html.Paragraph]
    paragraph.textContent = msg
    paragraph
  }

  def getWebsocketUri(participant: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${dom.document.location.host}/rfm?name=$participant"
  }

}
