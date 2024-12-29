/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug
package internal

import parsley.debug.internal.Defaults._
import scalafx.beans.binding.Bindings
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.Pos
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.layout.{GridPane, Priority, VBox}
import scalafx.scene.text.{FontWeight, Text, TextFlow}

private [debug] class AttemptInfo(dtree: ObjectProperty[Option[DebugTree]])(implicit
  fontMult: Double
) extends ScrollPane {
  // Makes sure the content doesn't go off the sides:
  fitToWidth = true
  hgrow = Priority.Always

  background = DefaultBackground

  hbarPolicy = ScrollBarPolicy.Never
  vbarPolicy = ScrollBarPolicy.Never

  // Contents.
  // Finally set content to the list of attempts.
  content <== Bindings.createObjectBinding(
    () => {
      // We also want to reset the scrollbar to the top, too.
      vvalue = 0

      val allList = new VBox()

      if (dtree().isDefined) {
        for (att <- dtree().get.parseResults.map(new Attempt(_))) {
          att.prefHeight <== height
          allList.children.add(att)
        }
      }

      allList.delegate
    },
    dtree
  )
}

private [debug] class Attempt(att: ParseAttempt)(implicit fontMult: Double) extends GridPane {
  // Visual parameters.
  background = simpleBackground(if (att.success) SuccessColour else FailureColour)

  hgap = relativeSize(1)
  vgap = relativeSize(0.5)

  padding = simpleInsets(1)

//  prefWidth <== outer.width

  hgrow = Priority.Always

  alignment = Pos.CenterLeft

  // Contents.
  add(
    new TextFlow(
      new Text {
        this.text = if (att.fromOffset == att.toOffset) {
          "*** Parser did not consume input. ***"
        } else {
          val untilLB  = att.rawInput.takeWhile(!"\r\n".contains(_))
          val addition = if (att.rawInput.length > untilLB.length) " [...]" else ""

          s"\"${untilLB + addition}\""
        }
        font = monoFont(1, FontWeight.Bold)
      }
    ),
    1,
    0
  )

  add(
    new TextFlow(
        new Text {
            text =
              if (att.fromOffset == att.toOffset) "N/A"
              else s"${att.fromPos} to ${att.toPos}"
            font = defaultFont(1)
        }
    ),
    1,
    1
  )

  add(
    {
      val resultText = new Text {
        text = if (att.success) "Result: " else ""
        font = defaultFont(1, FontWeight.Bold)
      }
      val itemText   = new Text { text = att.result.mkString; font = monoFont(1) }
      new TextFlow(resultText, itemText)
    },
    columnIndex = 1,
    rowIndex = 2
  )

  add(
    new Text {
      text = if (att.success) "✓" else "✗"
      font = defaultFont(3, FontWeight.Black)
    },
    0,
    0,
    1,
    3
  )
}
