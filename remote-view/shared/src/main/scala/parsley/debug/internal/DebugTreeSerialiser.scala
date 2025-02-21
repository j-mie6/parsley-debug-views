/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

import java.io.Writer

import upickle.default.{ReadWriter => RW, macroRW}

import parsley.debug.DebugTree
import parsley.debug.ParseAttempt

/**
  * Case class instance of the DebugTree structure.
  * 
  * This will be serialised to JSON structures of the following form.
  * 
  * {
  *   name            : String
  *   internal        : String
  *   success         : Boolean
  *   childId         : Long
  *   fromOffset      : Int
  *   toOffset        : Int
  *   input           : String
  *   children        : [DebugTree]
  *   doesNeedBubbling: Boolean
  * }
  *
  * @param name (Possibly) User defined name.
  * @param internal Internal parser name.
  * @param success Did the parser succeed.
  * @param childId The unique child number of this node.
  * @param fromOffset Offset into the input in which this node's parse attempt starts.
  * @param toOffset Offset into the input in which this node's parse attempt finished.
  * @param input The input string passed to the parser.
  * @param children An array of child nodes.
  * @param doesNeedBubbling Does this node need to bubble up to the parent?
  */
private case class SerialisableDebugTree(name: String, internal: String, success: Boolean, childId: Long, fromOffset: ParseAttempt.Offset, toOffset: ParseAttempt.Offset, children: List[SerialisableDebugTree], doesNeedBubbling: Boolean)

private object SerialisableDebugTree {
  implicit val rw: RW[SerialisableDebugTree] = macroRW
}

private case class SerialisablePayload(input: String, root: SerialisableDebugTree)

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
    SerialisableDebugTree(
      tree.parserName,
      tree.internalName,
      tree.parseResults.exists(_.success),
      tree.childNumber.getOrElse(-1), 
      tree.parseResults.map(_.fromOffset).getOrElse(-1),
      tree.parseResults.map(_.toOffset).getOrElse(-1),
      children,
      tree.doesNeedBubbling
    )
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