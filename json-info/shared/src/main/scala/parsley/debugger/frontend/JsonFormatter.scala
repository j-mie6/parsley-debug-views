package parsley.debugger.frontend

import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.ToJson.Implicits._

/** A frontend that emits a JSON-format string representing the parse tree.
  *
  * Unlike most other frontends, this frontend will call a continuation that will have the JSON value passed into after
  * it has been produced by traversing the tree.
  *
  * This continuation must return unit, as there are not many other ways to make it useful other than making this
  * impure. It is recommended that if one wants to extract multiple inputs from the same frontend with the same
  * continuation, that it is recorded to some kind of list or iterable structure.
  */
class JsonFormatter(cont: ujson.Value => Unit) extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit =
    cont(
      ujson.Obj(
        "input" -> input.toJson,
        "parse" -> tree.toJson
      )
    )
}

object JsonFormatter {
  def apply(cont: ujson.Value => Unit): JsonFormatter = new JsonFormatter(cont)
}

/** A version of [[JsonFormatter]] that takes a JSON string instead of a [[ujson.Value]] object. */
class JsonStringFormatter(cont: String => Unit, indent: Int = 2, escapeUnicode: Boolean = false)
  extends JsonFormatter(json => cont(json.render(indent, escapeUnicode = escapeUnicode)))

object JsonStringFormatter {
  def apply(cont: String => Unit, indent: Int = 2, escapeUnicode: Boolean = false): JsonStringFormatter =
    new JsonStringFormatter(cont, indent, escapeUnicode)
}
