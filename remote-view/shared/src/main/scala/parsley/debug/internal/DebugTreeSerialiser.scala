/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

import parsley.debug.DebugTree
import java.io.Writer

import upickle.default.{ReadWriter => RW, macroRW}
import upickle.default

private case class DebugTreeInstance(name: String, internal: String, success: Boolean, number: Long, input: String, children: List[DebugTreeInstance])

private object DebugTreeInstance {
  implicit val rw: RW[DebugTreeInstance] = macroRW
}


package DebugTreeSerialiser {

  private def constructDebugTree(tree: DebugTree): DebugTreeInstance = {
    val children: List[DebugTreeInstance] = tree.nodeChildren.map(constructDebugTree(_))
    DebugTreeInstance(tree.parserName, tree.internalName, tree.parseResults.exists(_.success), tree.childNumber.getOrElse(0), tree.fullInput, children)
  }

  def writeJSON(file: Writer, tree: DebugTree): Unit = {
    val treeRoot: DebugTreeInstance = this.constructDebugTree(tree)
    upickle.default.writeTo(treeRoot, file)
  }

  def toJSON(tree: DebugTree): String = {
    val treeRoot: DebugTreeInstance = this.constructDebugTree(tree)
    upickle.default.write(treeRoot)
  }
}