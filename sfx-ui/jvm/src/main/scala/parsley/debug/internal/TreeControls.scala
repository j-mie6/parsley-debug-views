/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

import scalafx.beans.binding.Bindings
import scalafx.beans.property.DoubleProperty
import scalafx.geometry.Pos
import scalafx.scene.control.{Button, Slider}
import scalafx.scene.layout.GridPane

private [debug] class TreeControls(
  view: TreeDisplay,
  zoomLevel: DoubleProperty
) extends GridPane { outer =>
  add(view, 0, 0, 2, 1)

  alignment = Pos.Center

  // Buttons with fold controls.
  private val unfold = new Button {
    text = "Unfold All"
    onAction = _ => view.unfoldAll()

    prefWidth <== Bindings.createDoubleBinding(
      () => outer.layoutBounds().getWidth / 2,
      outer.layoutBounds
    )

    alignmentInParent = Pos.Center
  }

  private val fold = new Button {
    text = "Fold All"
    onAction = _ => view.foldAll()

    prefWidth <== Bindings.createDoubleBinding(
      () => outer.layoutBounds().getWidth / 2,
      outer.layoutBounds
    )

    alignmentInParent = Pos.Center
  }

  private val zoomBar = new Slider {
    max = 1
    min = 0.1
    value = 1

    showTickLabels = true
    showTickMarks = true
    majorTickUnit = 0.1
    minorTickCount = 1
  }

  zoomLevel <== zoomBar.value

  add(zoomBar, 0, 1, 2, 1)
  add(unfold, 0, 2)
  add(fold, 1, 2)
}
