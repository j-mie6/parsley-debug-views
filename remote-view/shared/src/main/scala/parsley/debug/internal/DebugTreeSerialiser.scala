/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

import parsley.debug.DebugTree
import java.io.Writer

import upickle.default.{ReadWriter => RW, macroRW}

/**
  * Case class instance of the DebugTree structure.
  * 
  * This will be serialised by upickle to JSON structures of the form -
  * 
  * {
  *   name      : String
  *   internal  : String
  *   success   : Boolean
  *   number    : Long
  *   input     : String
  *   children  : [DebugTree]
  * }
  *
  * @param name (Possibly) User defined name.
  * @param internal Internal parser name.
  * @param success Did the parser succeed.
  * @param number The unique child number of this node.
  * @param input The input string passed to the parser.
  * @param children An array of child nodes.
  */
private case class DebugTreeInstance(name: String, internal: String, success: Boolean, number: Long, input: String, children: List[DebugTreeInstance])

private object DebugTreeInstance {
  implicit val rw: RW[DebugTreeInstance] = macroRW
}

/**
  * The Debug Tree Serialiser contains methods for transforming the parsley.debug.DebugTree to a
  * JSON stream.
  * 
  * Methods 
  * 
  * - writeJSON(file: Writer, tree: DebugTree): Unit
  * 
  * - toJSON(tree: DebugTree): String
  */
object DebugTreeSerialiser {

  private def constructDebugTree(tree: DebugTree): DebugTreeInstance = {
    val children: List[DebugTreeInstance] = tree.nodeChildren.map(constructDebugTree(_))
    DebugTreeInstance(tree.parserName, tree.internalName, tree.parseResults.exists(_.success), tree.childNumber.getOrElse(0), tree.fullInput, children)
  }

  /**
    * Write a DebugTree to a writer stream as JSON.
    *
    * @param file A valid writer object.
    * @param tree The DebugTree.
    */
  def writeJSON(file: Writer, tree: DebugTree): Unit = {
    val treeRoot: DebugTreeInstance = this.constructDebugTree(tree)
    upickle.default.writeTo(treeRoot, file)
  }

  /**
    * Transform the DebugTree to a JSON string.
    *
    * @param tree The DebugTree
    * @return JSON formatted String
    */
  def toJSON(tree: DebugTree): String = {
    val treeRoot: DebugTreeInstance = this.constructDebugTree(tree)
    upickle.default.write(treeRoot)
  }
}