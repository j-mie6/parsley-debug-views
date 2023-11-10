package parsley.debugger.frontend

import io.circe._
import io.circe.syntax._

import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.CJson._

/** A frontend that emits a JSON-format string representing the parse tree.
  *
  * Unlike most other frontends, this frontend will call a continuation that will have the JSON value passed into after
  * it has been produced by traversing the tree.
  *
  * This continuation must return unit, as there are not many other ways to make it useful other than making this
  * impure. It is recommended that if one wants to extract multiple inputs from the same frontend with the same
  * continuation, that it is recorded to some kind of list or iterable structure.
  */
sealed class JsonFormatter private[frontend] (cont: Output => Unit) extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit =
    cont(
      JsonObject(
        "input" -> input.toJSON,
        "parse" -> tree.toJSON
      ).asJson
    )
}

object JsonFormatter {
  def apply(cont: Output => Unit): JsonFormatter = new JsonFormatter(cont)
}

/** A version of [[JsonFormatter]] that emits a JSON string instead of a [[io.circe.Json]] object. */
final class JsonStringFormatter private[frontend] (cont: String => Unit, indent: Int, escapeUnicode: Boolean)
  extends JsonFormatter(json =>
    cont(
      json.printWith(
        new Printer(
          indent = List.fill(indent)(' ').mkString(""), // .repeat() is not portable
          escapeNonAscii = escapeUnicode,
          dropNullValues = false
        )
      )
    )
  )

object JsonStringFormatter {
  def apply(cont: String => Unit, indent: Int = 2, escapeUnicode: Boolean = false): JsonStringFormatter =
    new JsonStringFormatter(cont, indent, escapeUnicode)
}
