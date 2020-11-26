package org.eu.nl.laszlo.rfm

import upickle.default.{macroRW, ReadWriter => RW}

import scala.util.Random

object Protocol {

  //model
  object Magnet {
    def apply(id: Int, text: CharSequence): Magnet = {
      Magnet(makeId(id, text), text.toString)
    }

    def makeId(id: Int, text: CharSequence): String = text.toString + id

    implicit def rw: RW[Magnet] = macroRW
  }

  final case class Magnet(handle: String, text: String)

  object Point {
    val origin: Point = Point(0, 0)

    implicit def rw: RW[Point] = macroRW

    def randomWithin(bounds: Square): Point = bounds.randomPointWithin()

  }

  final case class Point(x: Int, y: Int) {
    def isInSquare(s: Square): Boolean = s.contains(this)

    def terse: String = s"$x,$y"
  }

  object Square {
    implicit def rw: RW[Square] = macroRW
  }

  final case class Square(nearest: Point, farthest: Point) {
    private val xRange = nearest.x to farthest.x
    private val yRange = nearest.y to farthest.y

    private val width: Int = xRange.last - xRange.head
    private val height: Int = yRange.last - yRange.head

    def contains(p: Point): Boolean = {
      xRange.contains(p.x) && yRange.contains(p.y)
    }

    def randomPointWithin(): Point = {
      Point(
        Random.nextInt(width),
        Random.nextInt(height)
      )
    }

  }

  //commands
  object MagnetMessage {
    implicit def rw: RW[MagnetMessage] = RW.merge(Locate.rw, Grab.rw, Drag.rw, Release.rw)
  }

  sealed trait MagnetMessage

  case object Locate extends MagnetMessage {
    implicit def rw: RW[Locate.type] = macroRW
  }

  case object Grab extends MagnetMessage {
    implicit def rw: RW[Grab.type] = macroRW
  }

  case object Release extends MagnetMessage {
    implicit def rw: RW[Release.type] = macroRW
  }

  object Drag {
    implicit def rw: RW[Drag] = macroRW
  }

  case class Drag(to: Point) extends MagnetMessage

  //requests

  sealed trait MagnetCommand {
    def magnetHandle: String

    def message: MagnetMessage
  }

  trait InternalRequest {
    def name: String
  }

  case class ExternalRequestWrapper(name: String, request: Request) extends InternalRequest

  object Request {
    implicit def rw: RW[Request] = RW.merge(GetFullState.rw, GrabMagnet.rw, DragMagnet.rw, ReleaseMagnet.rw)
  }

  sealed trait Request {
    def toExternalRequestWrapper(name: String): ExternalRequestWrapper = ExternalRequestWrapper(name, this)
  }

  case object GetFullState extends Request {
    implicit def rw: RW[GetFullState.type] = macroRW
  }

  object GrabMagnet {
    implicit def rw: RW[GrabMagnet] = macroRW
  }

  final case class GrabMagnet(magnetHandle: String) extends Request with MagnetCommand {
    override def message: MagnetMessage = Grab
  }

  object DragMagnet {
    implicit def rw: RW[DragMagnet] = macroRW
  }

  final case class DragMagnet(magnetHandle: String, toPoint: Point) extends Request with MagnetCommand {
    override def message: MagnetMessage = Drag(toPoint)
  }

  object ReleaseMagnet {
    implicit def rw: RW[ReleaseMagnet] = macroRW
  }

  final case class ReleaseMagnet(magnetHandle: String) extends Request with MagnetCommand {
    override def message: MagnetMessage = Release
  }


  //responses
  object Response {
    implicit def rw: RW[Response] = RW.merge(NewPositions.rw, MagnetGrabbed.rw, MagnetReleased.rw, AggregateStateChange.rw)
  }

  sealed trait Response {
    def asAggregate: AggregateStateChange
  }

  object NewPositions {
    implicit def rw: RW[NewPositions] = macroRW
  }

  final case class NewPositions(positions: Map[Magnet, Point], partial: Boolean) extends Response {
    override def asAggregate: AggregateStateChange = AggregateStateChange(moved = Some(this))

    def updatedWith(otherPositions: Map[Magnet, Point]): NewPositions = copy(positions ++ otherPositions)
  }

  object MagnetGrabbed {
    implicit def rw: RW[MagnetGrabbed] = macroRW
  }

  final case class MagnetGrabbed(magnet: Magnet, grabber: String) extends Response {
    override def asAggregate: AggregateStateChange = AggregateStateChange(grabbed = Set(this))
  }

  object MagnetReleased {
    implicit def rw: RW[MagnetReleased] = macroRW
  }

  final case class MagnetReleased(magnet: Magnet) extends Response {
    override def asAggregate: AggregateStateChange = AggregateStateChange(released = Set(this))
  }

  object AggregateStateChange {
    implicit def rw: RW[AggregateStateChange] = macroRW
  }

  case class AggregateStateChange(metadata: Option[Square] = None,
                                  moved: Option[NewPositions] = None,
                                  grabbed: Set[MagnetGrabbed] = Set.empty,
                                  released: Set[MagnetReleased] = Set.empty) extends Response {
    override def asAggregate: AggregateStateChange = this

    /**
      * add the response to this AggregateStateChange
      *
      * @param response the response to add to the current state
      * @return the new state
      */
    def add(response: Response): AggregateStateChange = response match {
      case np@NewPositions(_, partial) if moved.isEmpty || !partial =>
        copy(moved = Some(np))
      case NewPositions(positions, _) =>
        copy(moved = moved.map(_.updatedWith(positions)))
      case mg@MagnetGrabbed(magnet, _) if released.exists(_.magnet == magnet) =>
        copy(grabbed = grabbed + mg, released = released.filterNot(_.magnet == magnet))
      case mg: MagnetGrabbed =>
        copy(grabbed = grabbed + mg)
      case mr@MagnetReleased(magnet) if grabbed.exists(_.magnet == magnet) =>
        copy(released = released + mr, grabbed = grabbed.filterNot(_.magnet == magnet))
      case mr: MagnetReleased =>
        copy(released = released + mr)
      case AggregateStateChange(md, onp, g, r) =>
        AggregateStateChange(
          md.orElse(metadata),
          onp match {
            case Some(np) if moved.isDefined => moved.map(_.updatedWith(np.positions))
            case _ => moved.orElse(onp)
          },
          (grabbed ++ g).filterNot(mg => r.exists(_.magnet == mg.magnet)),
          (released ++ r).filterNot(mr => g.exists(_.magnet == mr.magnet))
        )
    }

  }

}