/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.consolepretty.*

/** A console pretty-printer for the debugger.
  *
  * It is recommended that all memory-heavy types (e.g. closures) are not stored explicitly. Consult the documentation
  * on attaching debuggers to find out how to prevent that.
  */
final class ConsolePrettyPrinter private[frontend] (ioF: String => Unit) extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    ioF(s"${tree.parserName}'s parse tree for input:\n\n$input\n\n")
    ioF(tree.pretty + "\n")
  }
}

object ConsolePrettyPrinter {
  /** Create a `println` console pretty printer. */
  def apply(): ConsolePrettyPrinter = apply(println(_))

  /** Create a string pretty-printer that takes an arbitrary impure string function. */
  def apply(ioF: String => Unit) = new ConsolePrettyPrinter(ioF)
}
