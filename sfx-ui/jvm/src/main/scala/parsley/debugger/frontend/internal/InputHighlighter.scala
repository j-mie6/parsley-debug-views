/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend.internal

import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.Defaults.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.layout.Priority
import scalafx.scene.paint.Color
import scalafx.scene.text.{FontWeight, Text, TextFlow}

private[frontend] class InputHighlighter(
  fullInput: String,
  selected: ObjectProperty[Option[DebugTree]]
)(implicit fontMult: Double)
  extends ScrollPane { outer =>
  // Produce our local bindings for the text contents of our three texts.
  private val beforeBinding = Bindings.createStringBinding(
    () => {
      if (selected().isDefined) fullInput.slice(0, selected().get.parseResults.get.fromOffset)
      else fullInput
    },
    selected
  )
  private val duringBinding = Bindings.createStringBinding(
    () => {
      if (selected().exists(_.parseResults.exists(att => att.fromOffset < att.toOffset)))
        fullInput.slice(
          selected().get.parseResults.get.fromOffset,
          selected().get.parseResults.get.toOffset
        )
      else ""
    },
    selected
  )
  private val afterBinding  = Bindings.createStringBinding(
    () => {
      if (selected().isDefined)
        fullInput.slice(selected().get.parseResults.get.toOffset, fullInput.length)
      else ""
    },
    selected
  )

  // Produce the textFlow object needed for this display.
  private val before = new Text {
    text <== beforeBinding
    font = monoFont(1.25)
  }

  private val during = new Text {
    text <== duringBinding
    font = monoFont(1.25, FontWeight.Black)
    fill = Color.Green
    underline = true
  }

  private val after = new Text {
    text <== afterBinding
    font = monoFont(1.25)
  }

  private val completed = new TextFlow(before, during, after)

  // The scrollbars don't always have to be there.
  hbarPolicy = ScrollBarPolicy.AsNeeded
  vbarPolicy = ScrollBarPolicy.AsNeeded

  // Finally set up our scrollable.
  hgrow = Priority.Always

  background = DefaultBackground

  content = completed
}
