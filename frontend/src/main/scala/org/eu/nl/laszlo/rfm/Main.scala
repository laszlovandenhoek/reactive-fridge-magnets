package org.eu.nl.laszlo.rfm

import java.util.UUID

import org.eu.nl.laszlo.rfm.Protocol._
import org.scalajs.dom
import org.scalajs.dom._
import upickle.default._

object Main {

  val magnetType = "text/magnet"

  val messages: html.Div = dom.document.getElementById("messages").asInstanceOf[html.Div]
  val canvas: html.Div = dom.document.getElementById("canvas").asInstanceOf[html.Div]

  canvas.style.zIndex = "-1"
  canvas.style.height = "720px"
  canvas.style.width = "1280px"
  canvas.style.border = "1px solid black"

  val name: String = UUID.randomUUID().toString

  def writeToArea(text: String): Unit =
    messages.innerHTML = text

  def main(args: Array[String]): Unit = {
    joinChat(name)
  }

  def joinChat(name: String): Unit = {
    writeToArea(s"Trying to join chat as '$name'...")
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

    canvas.ondragenter = { e: DragEvent =>
      console.info("ondragEnter dataTransfer", e.dataTransfer)
      for (t <- e.dataTransfer.types) {
        console.info("ondragEnter dataTransfer.type", t)
      }
      e.preventDefault();
    }

    canvas.ondragover = { e: DragEvent =>
      e.dataTransfer.dropEffect = "move"

      e.preventDefault()
    }

    canvas.ondrop = { e: DragEvent =>
      val droppedMagnetHandle = e.dataTransfer.getData(magnetType)
      console.info("onDrop", droppedMagnetHandle)
      stopDragging(droppedMagnetHandle)
    }

    def processResponse(r: Protocol.Response): Unit = {
      r match {
        case Protocol.AggregateStateChange(meta, moved, grabbed, released) =>
          (grabbed ++ released ++ moved.toSet).foreach(processResponse)
        case Protocol.NewPositions(positions, partial) =>
          console.info(if (partial) "updated positions" else "new positions", positions.map({ case (m, p) => s"${m.handle} -> ${p.terse}" }).toString)
          positions.foreach((moveMagnet _).tupled)
        case Protocol.MagnetGrabbed(magnet, grabber) =>
          console.info("grabbed", magnet.handle, grabber)
          grabMagnet(magnet, grabber)
        case Protocol.MagnetReleased(magnet) =>
          console.info("released", magnet.handle)
          releaseMagnet(magnet)
      }
    }

    def getMagnet(magnet: Magnet): html.Paragraph = {
      val existingP = dom.document.getElementById(magnet.handle).asInstanceOf[html.Paragraph]
      if (existingP != null) return existingP


      val newP = p(magnet.text)
      newP.id = magnet.handle
      newP.draggable = true

      newP.ondragstart = { event: DragEvent =>
        console.info("ondragStart", magnet.handle, event.target)
        event.dataTransfer.setData(magnetType, magnet.handle)
        notifyStartDragging(magnet.handle)
      }

      newP.ondrag = { event: DragEvent =>
        val point = Point(event.pageX.toInt - 24, event.pageY.toInt - 24)
        console.info("ondrag", magnet.handle, point.terse)
        //TODO: debounce moves
        chat.send(write(DragMagnet(magnet.handle, point)))
      }
      canvas.appendChild(newP)
      newP
    }

    //client actions

    def notifyStartDragging(magnetHandle: String): Unit = {
      chat.send(
        write(GrabMagnet(magnetHandle))
      )
    }

    def stopDragging(magnetHandle: String): Unit = {
      console.info("stop dragging", magnetHandle)
      chat.send(
        write(ReleaseMagnet(magnetHandle))
      )
    }


    //server response handling

    def moveMagnet(magnet: Magnet, point: Point): Unit = {
      val magnetHTML = getMagnet(magnet)
      magnetHTML.style.left = point.x.toString
      magnetHTML.style.top = point.y.toString
    }

    def grabMagnet(magnet: Magnet, grabber: String): Unit = {
      val paragraph = getMagnet(magnet)
      paragraph.style.borderColor = grabber.take(6)
      if (grabber != name) {
        paragraph.draggable = false
      }

    }

    def releaseMagnet(magnet: Magnet): Unit = {
      getMagnet(magnet).style.borderColor = null
      getMagnet(magnet).draggable = true
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
