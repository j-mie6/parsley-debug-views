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
  * This will be serialised to JSON structures of the following form.
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
private case class SerialisableDebugTree(name: String, internal: String, success: Boolean, number: Long, input: String, children: List[SerialisableDebugTree])

private object SerialisableDebugTree {
  implicit val rw: RW[SerialisableDebugTree] = macroRW
}

private case class SerialisablePayload(input: String, tree: SerialisableDebugTree)

private object SerialisablePayload {
  implicit val rw: RW[SerialisablePayload] = macroRW
}

/**
  * The Debug Tree Serialiser contains methods for transforming the parsley.debug.DebugTree to a
  * JSON stream.
  */
object DebugTreeSerialiser {

  private def convertDebugTree(tree: DebugTree): SerialisableDebugTree = {
    val children: List[SerialisableDebugTree] = tree.nodeChildren.map(convertDebugTree(_))
    SerialisableDebugTree(tree.parserName, tree.internalName, tree.parseResults.exists(_.success), tree.childNumber.getOrElse(0), tree.fullInput, children)
  }

  /**
    * Write a DebugTree to a writer stream as JSON.
    *
    * @param file A valid writer object.
    * @param tree The DebugTree.
    */
  def writeJSON(file: Writer, input: String, tree: DebugTree): Unit = {
    val treeRoot: SerialisableDebugTree = this.convertDebugTree(tree)
    upickle.default.writeTo(SerialisablePayload(input, treeRoot), file)
  }

  /**
    * Transform the DebugTree to a JSON string.
    *
    * @param tree The DebugTree
    * @return JSON formatted String
    */
  def toJSON(input: String, tree: DebugTree): String = {
    val treeRoot: SerialisableDebugTree = this.convertDebugTree(tree)
    upickle.default.write(SerialisablePayload(input, treeRoot))
  }
}