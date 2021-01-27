package org.eu.nl.laszlo.rfm

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("draggabilly.pkgd.js", "Draggabilly")
//TODO?: emit a module https://www.scala-js.org/doc/project/module.html
//TODO: https://stackoverflow.com/questions/64740345/how-to-translate-javascript-class-to-scalajs
class Draggabilly(elem: Element, options: DraggabillyOptions /* TODO: optional */) extends js.Object {
  def on(eventName: String, eventListener: js.Function) = js.native
  def off(eventName: String, eventListener: js.Function) = js.native
  def once(eventName: String, eventListener: js.Function) = js.native

}

case class DraggabillyOptions(axis: String = "x or y",
                              containment: js.Any = (), //TODO: refine
                              grid: js.Any = (), //TODO: array [ x, y ]
                              handle: String = ".handle")
