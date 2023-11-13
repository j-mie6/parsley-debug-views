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
      val parentUuid = nextUid()

      dt.parseResults.get match {
        case ParseAttempt(ri, _, _, fp, tp, sc, res) =>
          <table class={if (dt.internalName != dt.parserName) "parser dotted" else "parser"}>
            <tr>
              <td id={s"parent_$parentUuid"} class={"attempt " + (if (sc) "success" else "failure")}>
                {if (dt.internalName != dt.parserName) <p class="nickname">{dt.parserName}</p> else <!-- Name intact. -->}
                <div class="info">
                  <table>
                    <tr>
                      <th>{s"${dt.parserName} (${dt.internalName})"}</th><td>{if (sc) "-[-{AMP}-]-#10004;" else "-[-{AMP}-]-#10008;"}</td>
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
                      {
                        funcTable +=
                          s"""var subs_$parentUuid = [];
                             |
                             |function unload_children_$parentUuid(e) {
                             |  subs_$parentUuid.forEach((f) => f());
                             |
                             |  var event = e ? e : window.event;
                             |  event.cancelBubble = true;
                             |  if (event.stopPropagation) event.stopPropagation();
                             |}
                             |""".stripMargin

                        dt.nodeChildren.iterator.map { case (_, p) =>
                          val uuid = nextUid()

                          funcTable +=
                            s"""function load_tree_$uuid(e) {
                               |  let target = document.getElementById("child_$uuid");
                               |
                               |  if (target && target.firstElementChild && target.firstElementChild.className.indexOf("unloaded") !== 1) {
                               |    target.innerHTML = `${dtToH.apply[DebugTree](p)}`;
                               |
                               |    if (target.hasAttribute("onclick")) {
                               |      target.removeAttribute("onclick");
                               |      target.addEventListener("click", load_tree_$uuid, false);
                               |    }
                               |
                               |    let parent = document.getElementById("parent_$parentUuid");
                               |    if (parent) parent.addEventListener("click", unload_children_$parentUuid, false);
                               |  } else if (target) {
                               |    target.innerHTML = `<div class="unloaded attempt"><p>${p.parserName}<br />(${p.internalName})</p></div>`;
                               |  }
                               |
                               |  var event = e ? e : window.event;
                               |  event.cancelBubble = true;
                               |  if (event.stopPropagation) event.stopPropagation();
                               |}
                               |
                               |function unload_tree_$uuid() {
                               |  let target = document.getElementById("child_$uuid");
                               |  if (target) target.innerHTML = `<div class="unloaded attempt"><p>${p.parserName}<br />(${p.internalName})</p></div>`;
                               |}
                               |
                               |funcs.push({ id: $uuid, fun: load_tree_$uuid });
                               |folds.push({ id: $uuid, fun: unload_tree_$uuid });
                               |subs_$parentUuid.push(unload_tree_$uuid);
                               |""".stripMargin

                          <td class="parser-child" id={s"child_$uuid"} onclick={s"load_tree_$uuid()"}>
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
