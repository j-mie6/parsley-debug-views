/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import javafx.embed.swing.JFXPanel
import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal._
import parsley.debugger.frontend.internal.Defaults._
import scalafx.application.Platform
import scalafx.beans.property.{DoubleProperty, ObjectProperty}
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.stage.Stage

/** ScalaFX (on JavaFX) renderer for debug trees. This frontend provides interactive visuals to explore the parse /
  * debug tree of a parser.
  *
  * Nodes can be expanded for information or moved around in order to re-organise them, and a live input display will
  * show what part of the input a parser has tried to parse when its respective node is selected.
  *
  * The primary mouse button (left-click for most users) is used to select nodes to show the parsed input for that node
  * and possibly its result.
  *
  * The secondary mouse button (right-click for most users) is used to fold or unfold nodes on the tree. It is possible
  * to fold both individual parser nodes (the red / green rectangles) and the named nodes (names on dotted borders).
  *
  * The Unfold All / Fold All buttons are relatively self-explanatory, as for the zoom bar located above said buttons.
  *
  * On the side of the window away from the tree display, a node info display and parse input display can be found. The
  * node info display shows more information about a parse attempt (its offsets, result, etc.) and the input display
  * shows where in the input the parse attempt was made, highlighted in bold, green, and underlined text within the rest
  * of the input.
  */
final class FxGUI(fontMult: Double) extends StatelessFrontend {
  implicit private val gfMult: Double = fontMult

  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    // This forces initialisation of JavaFX's internals.
    // We don't actually need this for anything other than that.
    new JFXPanel()

    val selectedTree: ObjectProperty[Option[DebugTree]] = ObjectProperty(None)
    val zoomLevel: DoubleProperty                       = DoubleProperty(1.0)

    val inputDisplay = new InputHighlighter(input, selectedTree)

    Platform.runLater {
      val rendered = new Stage {
        title = "Parsley Tree Visualisation"
        scene = new Scene(960, 600) {
          fill = DefaultBGColour

          content = new ThreeSplitPane(
            this,
            new TreeControls(new TreeDisplay(this, tree, selectedTree, zoomLevel), zoomLevel),
            new AttemptInfo(selectedTree),
            new HBox(inputDisplay)
          )
        }
      }

      rendered.showAndWait()
      // No need to exit.
    }
  }
}

object FxGUI {
  /** Create a new instance of [[FxGUI]] with a given font size multiplier. */
  def apply(mult: Double = 1.0): FxGUI = new FxGUI(mult)
}
