/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend.internal

import scala.collection.mutable
import scala.xml.*

import parsley.debugger.{DebugTree, ParseAttempt}

private[frontend] trait ToHTML[-V] {
  def apply[V1 <: V](x: V1)(implicit funcTable: mutable.Buffer[String]): Node
}

private[frontend] object ToHTML {
  implicit class ToHTMLOps[-V: ToHTML](x: V) {
    def toHTML(implicit funcTable: mutable.Buffer[String]): Node = implicitly[ToHTML[V]].apply[V](x)
  }

  private var uid: Long = -1L
  private def nextUid(): Long = this.synchronized {
    uid = uid + 1
    uid
  }

  val ampSeq: String = "-[-{AMP}-]-"
  val nlSeq: String  = "-[-{NL}-]-"

  // format: off
  implicit lazy val dtToH: ToHTML[DebugTree] = new ToHTML[DebugTree] {
    override def apply[V1 <: DebugTree](dt: V1)(implicit funcTable: mutable.Buffer[String]): Node = {
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
                      <th>Input:</th><td>{s"\"${ri.slice(fo, to + 1).replace("\r", "").replace("\n", nlSeq)}\""}</td>
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
                      {dt.nodeChildren.iterator.map { case (_, p) =>
                        val uuid = nextUid()

                        funcTable +=
                          s"""var loaded_$uuid = false;
                             |function load_tree_$uuid() {
                             |  if (!loaded_$uuid) {
                             |    document.getElementById("child_$uuid").innerHTML = `${dtToH.apply[DebugTree](p)}`;
                             |    document.getElementById("child_$uuid").removeAttribute("onclick");
                             |
                             |    loaded_uuid = true;
                             |  }
                             |}
                             |
                             |funcs.push({ id: $uuid, fun: load_tree_$uuid });
                             |""".stripMargin

                        <td id={s"child_$uuid"} onclick={s"load_tree_$uuid()"}>
                          <div class="unloaded attempt">
                            <p>{p.parserName}<br />({p.internalName})</p>
                          </div>
                        </td> }}
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
    }
  }
  // format: on
}
