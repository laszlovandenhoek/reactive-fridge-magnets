package org.eu.nl.laszlo.rfm

import java.util.UUID

import org.eu.nl.laszlo.rfm.Protocol._
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
    chat.onclose = { event: CloseEvent =>
      messages.insertBefore(p(s"Connection to chat lost. You can try to rejoin manually. Code: ${event.code}, reason: ${event.reason}, clean: ${event.wasClean}"), messages.firstChild)
    }


    def processResponse(r: Protocol.Response): Unit = {
      r match {
        case Protocol.AggregateStateChange(moved, grabbed, released) =>
          (grabbed ++ released ++ moved.toSet).foreach(processResponse)
        case Protocol.NewPositions(positions, partial) =>
          writeToArea(s"${if (partial) "updated positions" else "new positions"}: $positions")
          positions.foreach((moveMagnet _).tupled)
        case Protocol.MagnetGrabbed(magnet, grabber) =>
          writeToArea(s"$magnet grabbed by $grabber")
          grabMagnet(magnet)
        case Protocol.MagnetReleased(magnet) =>
          writeToArea(s"$magnet released")
          releaseMagnet(magnet)
      }
    }

    def writeToArea(text: String): Unit =
      messages.innerHTML = text

    def getMagnet(magnet: Magnet): html.Paragraph = {
      val existingP = dom.document.getElementById(magnet.handle).asInstanceOf[html.Paragraph]
      if (existingP != null) return existingP


      val newP = p(magnet.text)
      newP.id = magnet.handle
      newP.style.position = "absolute"
      newP.style.fontFamily = "sans-serif"
      newP.style.fontSize = "16px"
      newP.style.borderWidth = "3px"
      newP.style.borderStyle = "solid"
      newP.style.borderColor = "transparent"

      newP.onmousedown = { event: MouseEvent =>
        newP.draggable = true
        startDragging(magnet)
      }
      newP.onmouseup = { event: MouseEvent =>
        newP.draggable = false
        stopDragging(magnet)
      }

      //TODO: put on drag handler
      //TODO: debounce moves
      //TODO: translate moves to ws comms


      canvas.appendChild(newP)
      newP
    }

    //client actions

    def startDragging(magnet: Magnet): Unit = {
      chat.send(
        write(GrabMagnet(magnet))
      )
    }

    def stopDragging(magnet: Magnet): Unit = {
      chat.send(
        write(ReleaseMagnet)
      )
    }


    //server response handling

    def moveMagnet(magnet: Magnet, point: Point): Unit = {
      val magnetHTML = getMagnet(magnet)
      magnetHTML.style.left = point.x.toString
      magnetHTML.style.top = point.y.toString
    }

    def grabMagnet(magnet: Magnet): Unit = {
      getMagnet(magnet).style.borderColor = "red"
    }

    def releaseMagnet(magnet: Magnet): Unit = {
      getMagnet(magnet).style.borderColor = "transparent"
    }
  }

  def p(msg: String): html.Paragraph = {
    val paragraph = dom.document.createElement("p").asInstanceOf[html.Paragraph]
    paragraph.textContent = msg
    paragraph
  }

  def getWebsocketUri(participant: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${
      dom.document.location.host
    }/rfm?name=$participant"
  }

}
