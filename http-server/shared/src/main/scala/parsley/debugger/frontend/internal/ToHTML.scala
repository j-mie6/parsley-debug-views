package parsley.debugger.frontend.internal

import scala.xml._
import parsley.debugger.{DebugTree, ParseAttempt}

private[frontend] trait ToHTML[-V] extends (V => Node) {
  def apply(x: V): Node
}

private[frontend] object ToHTML {
  implicit class ToHTMLOps[-V: ToHTML](x: V) {
    def toHTML: Node = implicitly[ToHTML[V]].apply(x)
  }

  implicit lazy val dtToH: ToHTML[DebugTree] = (dt: DebugTree) =>
    dt.parseResults.get match {
      case ParseAttempt(ri, fo, to, fp, tp, sc, res) =>
        <div class={if (dt.internalName != dt.parserName) "parser dotted" else "parser"}>
          {if (dt.internalName != dt.parserName) <p class="nickname">{dt.parserName}</p> else <!-- Name intact. -->}
          <div class={"attempt " + (if (sc) "success" else "failure")}>
            <div class="info">
              <table>
                <tr>
                  <th>Input:</th><td>{s"\"${ri.slice(fo, to + 1)}\""}</td>
                </tr>

                <tr>
                  <th>Position:</th><td>{s"$fp to $tp"}</td>
                </tr>

                <tr>
                  <th>Result:</th><td>{if (sc) res.toString else "[N/A]"}</td>
                </tr>
              </table>
            </div>

            <p>
              {dt.internalName}<br />{if (sc) "Success" else "Failure"}
            </p>
          </div>
          {
          if (dt.nodeChildren.nonEmpty) {
          <div class="children">
            {dt.nodeChildren.iterator.map { case (_, p) => dtToH.apply(p) }}
          </div>
          } else
          <!-- No children. -->
        }
        </div>
    }
}
