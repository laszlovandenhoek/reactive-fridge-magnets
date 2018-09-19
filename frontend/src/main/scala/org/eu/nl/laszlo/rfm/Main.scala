package org.eu.nl.laszlo.rfm

import org.scalajs.dom
import org.scalajs.dom.{Document, console, document}

object Main {
  def main(args: Array[String]): Unit = {
    console.info("hello world!")
    appendPar(document.body, "Hello World")
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  def getWebsocketUri(document: Document, nameOfChatParticipant: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${dom.document.location.host}/chat?name=$nameOfChatParticipant"
  }

}
