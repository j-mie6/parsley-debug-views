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

  val ampSeq: String = "-[-{AMP}-]-"

  // format: off
  implicit lazy val dtToH: ToHTML[DebugTree] = (dt: DebugTree) =>
    dt.parseResults.get match {
      case ParseAttempt(ri, fo, to, fp, tp, sc, res) =>
        <table class={if (dt.internalName != dt.parserName) "parser dotted" else "parser"}>
          <tr>
            <td class={"attempt " + (if (sc) "success" else "failure")}>
              {if (dt.internalName != dt.parserName) <p class="nickname">{dt.parserName}</p> else <!-- Name intact. -->}
              <div class="info">
                <table>
                  <tr>
                    <th>{s"${dt.parserName} (${dt.internalName})"}</th><td>{if (sc) "-[-{AMP}-]-#10004;" else "-[-{AMP}-]-#10008;"}</td>
                  </tr>

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

              <p class="overview">
                {dt.internalName}<br />{if (sc) "-[-{AMP}-]-#10004;" else "-[-{AMP}-]-#10008;"}
              </p>
            </td>
          </tr>
          <tr>
            <td>
              {
              if (dt.nodeChildren.nonEmpty) {
              <table class="children">
                <tr>
                  {dt.nodeChildren.iterator.map { case (_, p) => <td>{dtToH.apply(p)}</td> }}
                </tr>
              </table>
              } else {
              <!-- No children. -->
              }
              }
            </td>
          </tr>
        </table>
    }
  // format: on
}
