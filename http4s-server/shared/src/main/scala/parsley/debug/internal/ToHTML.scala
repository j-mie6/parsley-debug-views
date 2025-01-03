/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug
package internal

import scala.collection.mutable
import scala.xml.*

private [debug] trait ToHTML[-V] {
  def apply[V1 <: V](x: V1, funcTable: mutable.Buffer[String]): Node
}

private [debug] object ToHTML {
  implicit class ToHTMLOps[-V: ToHTML](x: V) {
    def toHTML(funcTable: mutable.Buffer[String]): Node = implicitly[ToHTML[V]].apply[V](x, funcTable)
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
    override def apply[V1 <: DebugTree](dt: V1, funcTable: mutable.Buffer[String]): Node = {
      val parentUuid = nextUid()
      val uname      = s"${dt.internalName}${if (dt.childNumber.isDefined) s" (${dt.childNumber.get})" else ""}"

      dt.parseResults.get match {
        case ParseAttempt(ri, _, _, fp, tp, sc, res) =>
          <table class={if (dt.internalName != dt.parserName) "parser dotted" else "parser"}>
            <tr>
              <td id={s"parent_$parentUuid"} class={"attempt " + (if (sc) "success" else "failure")}>
                {if (dt.internalName != dt.parserName) <p class="nickname">{dt.parserName}</p> else <!-- Name intact. -->}
                <div class="info">
                  <table>
                    <tr>
                      <th>{uname}</th><td>{if (sc) "-[-{AMP}-]-#10004;" else "-[-{AMP}-]-#10008;"}</td>
                    </tr>

                    <tr>
                      <th>Input:</th><td>{s"\"${ri.replace("\r", "").replace("\n", nlSeq)}\""}</td>
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
                  {uname}<br />{if (sc) "-[-{AMP}-]-#10004;" else "-[-{AMP}-]-#10008;"}
                </p>
              </td>
            </tr>
            <tr>
              <td>
                {
                if (dt.nodeChildren.nonEmpty) {
                  <table class="children">
                    <tr>
                      {
                        dt.nodeChildren.iterator.map { p =>
                          val uuid = nextUid()

                          funcTable +=
                            s"""os[$uuid] = [`${dtToH.apply[DebugTree](p, funcTable)}`, $parentUuid];
                               |fs[$uuid] = `<div class="unloaded attempt"><p>${p.parserName}<br />(${p.internalName})</p></div>`;
                               |asb($parentUuid, () => unc($uuid));
                               |""".stripMargin

                          <td class="parser-child" id={s"child_$uuid"} onclick={s"lc($uuid)(undefined)"}>
                            <div class="unloaded attempt">
                              <p>{p.parserName}<br />({p.internalName})</p>
                            </div>
                          </td> }
                      }
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
