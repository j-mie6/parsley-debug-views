/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend.internal

import scala.annotation.tailrec
import scala.collection.mutable

import parsley.debugger.{DebugTree, ParseAttempt}

object consolepretty {
  // Utility class for aiding in the toString method for debug trees.
  private[parsley] case class PrettyPrintHelper(
    acc: mutable.StringBuilder,
    indents: Vector[String]
  ) {
    // Indent a string with the given indenting delimiters.
    def bury(str: String, withMark: Boolean = true): Unit = {
      val pretty =
        if (indents.isEmpty) str
        else if (withMark) indents.init.mkString + "+-" + str
        else indents.mkString + str

      acc.append(pretty + "\n")
    }

    // Add a new indent delimiter to the current helper instance.
    // The accumulator is shared between new instances.
    def addIndent(): PrettyPrintHelper =
      PrettyPrintHelper(acc, indents :+ "| ")

    // Adds a two-blank-space indent instead for the last child of a node.
    def addBlankIndent(): PrettyPrintHelper =
      PrettyPrintHelper(acc, indents :+ "  ")
  }

  implicit class TreePrinter(dt: DebugTree) {
    def pretty: String =
      prettyPrint(PrettyPrintHelper(new StringBuilder, Vector.empty)).acc.dropRight(1).toString()

    private def prettyPrint(helper: PrettyPrintHelper): PrettyPrintHelper = {
      val results = dt.parseResults.map(printParseAttempt).mkString
      helper.bury(s"[ ${dt.parserName} ]: $results")
      printChildren(helper, dt.nodeChildren.toList)
      helper
    }

    // Print a parse attempt in a human-readable way.
    private def printParseAttempt(attempt: ParseAttempt): String =
      s"(\"${attempt.rawInput}\" [${attempt.fromPos} -> ${attempt.toPos}], ${if (attempt.success)
          s"Success - [ ${attempt.result.get} ]"
        else "Failure"})"

    // Print all the children, remembering to add a blank indent for the last child.
    @tailrec private def printChildren(
      helper: PrettyPrintHelper,
      children: List[(String, DebugTree)]
    ): Unit =
      children match {
        case (_, t) :: Nil =>
          helper.bury("|", withMark = false)
          new TreePrinter(t).prettyPrint(helper.addBlankIndent())
          () // XXX: Silences discarded non-unit value warning.
        case (_, t) :: xs  =>
          helper.bury("|", withMark = false)
          new TreePrinter(t).prettyPrint(helper.addIndent())
          printChildren(helper, xs)
        case Nil           => ()
      }
  }
}
