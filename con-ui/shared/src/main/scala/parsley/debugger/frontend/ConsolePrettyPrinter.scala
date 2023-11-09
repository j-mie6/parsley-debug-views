/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.consolepretty._

/** A console pretty-printer for the debugger.
  *
  * Will automatically print the debug tree into stdout after a parser has finished running.
  *
  * Technically not a GUI.
  */
case object ConsolePrettyPrinter extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    println(s"${tree.parserName}'s parse tree for input:\n\n${input}\n")
    println(tree.pretty)
  }
}
