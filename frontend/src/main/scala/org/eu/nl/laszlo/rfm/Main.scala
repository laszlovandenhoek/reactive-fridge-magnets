package org.eu.nl.laszlo.rfm

import org.scalajs.dom
import dom.document
import dom.console

import scala.scalajs.js

object Main extends js.JSApp {
  def main(): Unit = {
    console.info("hello world!")
    appendPar(document.body, "Hello World")
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }
}
