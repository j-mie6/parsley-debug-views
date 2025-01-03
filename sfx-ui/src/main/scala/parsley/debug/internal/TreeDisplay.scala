/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug
package internal

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.layout
import parsley.debug.internal.Defaults.*
import parsley.debug.internal.TreeDisplay.mkTree
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, DoubleProperty, ObjectProperty}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.{Group, Scene}
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.input.MouseButton
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.scene.text.{FontWeight, Text}
import scalafx.scene.transform.Scale

import scala.collection.mutable
import scalafx.beans.binding.ObjectBinding

private [debug] class TreeDisplay(
  outer: Scene,
  tree: DebugTree,
  selected: ObjectProperty[Option[DebugTree]],
  zoomLevel: DoubleProperty
)(implicit fontMult: Double)
  extends ScrollPane {
  // Set visual parameters.
  prefHeight <== outer.height

  alignmentInParent = Pos.Center

  hbarPolicy = ScrollBarPolicy.AsNeeded
  vbarPolicy = ScrollBarPolicy.AsNeeded

  background = DefaultBackground

  implicit private val foldSetters: mutable.ListBuffer[BooleanProperty] =
    new mutable.ListBuffer()

  private val treeView = new Group(mkTree(tree, selected))
  treeView.transforms = Seq(
    new Scale(1, 1) {
      x <== zoomLevel
      y <== zoomLevel
      pivotX <== Bindings.createDoubleBinding(
        () => treeView.layoutBounds().getWidth * hvalue(),
        treeView.layoutBounds,
        hvalue
      )
      pivotY <== Bindings.createDoubleBinding(
        () => treeView.layoutBounds().getHeight * vvalue(),
        treeView.layoutBounds,
        vvalue
      )
    }
  )

  content = treeView

  hvalue = 0.5 // Set the scroll to the centre horizontally.

  def foldAll(): Unit = {
    // False hides things, so is the folded state.
    foldSetters.head() = false
    for (unfolded <- foldSetters.tail) {
      unfolded() = false
    }
  }

  def unfoldAll(): Unit = {
    // True is the unfolded state.
    foldSetters.head() = false
    for (unfolded <- foldSetters.tail) {
      unfolded() = true
    }
    foldSetters.head() = true
  }
}

private [debug] object TreeDisplay {
  private def mkDTreeRect(
    dtree: DebugTree,
    selected: ObjectProperty[Option[DebugTree]]
  )(implicit fontMult: Double): Pane = {
    // Shows the displayed name (renamed or otherwise) of the parser that produced the result tied
    // to this visual tree node.
    val uname = s"${dtree.internalName}${if (dtree.childNumber.isDefined) s" (${dtree.childNumber.get})" else ""}"

    val nameText = new Text {
      text = uname
      font = defaultFont(1, FontWeight.Black)
      alignmentInParent = Pos.Center
    }

    // Sets the colour of the rectangle to the respective success or failure colour depending on
    // if the parse result of the tree node was a success or failure.
    val colourBinding = Bindings.createObjectBinding(
      () => {
        if (selected().contains(dtree)) simpleBackground(Color.Yellow).delegate
        else
          simpleBackground(
            dtree.parseResults
              .map(_.success)
              .orElse(Some(false))
              .map(if (_) SuccessColour else FailureColour)
              .get
          ).delegate
      },
      selected
    )

    // A simple textual indicator of successes, for easier use by red-green colourblind users.
    val succText = new Text {
      text = if (dtree.parseResults.exists(_.success)) "✓" else "✗"
      font = defaultFont(1)
      alignmentInParent = Pos.Center
    }

    // The display rectangle for this tree node.
    val pane = new VBox {
      padding = simpleInsets(0.5)
      spacing = relativeSize(0.5)
      alignmentInParent = Pos.Center
      alignment = Pos.Center
      hgrow = Priority.Always
      background <== colourBinding

      onMouseClicked = event => {
        if (event.getButton == MouseButton.Primary.delegate) {
          if (selected().contains(dtree)) selected() = None
          else selected() = Some(dtree)
        }
      }
    }

    pane.children.addAll(nameText, succText)
    pane
  }

  def mkTree(
    dtree: DebugTree,
    selected: ObjectProperty[Option[DebugTree]]
  )(implicit
    folds: mutable.ListBuffer[BooleanProperty],
    fontMult: Double
  ): Pane = {
    // Start with the current tree node's rectangle...
    val rootNode = mkDTreeRect(dtree, selected)
    val columns  = dtree.nodeChildren.size

    val treeGrid = new GridPane {
      hgap = relativeSize(1)
      vgap = relativeSize(1)
      alignmentInParent = Pos.Center
      background = DefaultBackground
    }

    // ... then get all the rectangles of the child nodes.
    treeGrid.add(rootNode, 0, 0, Math.max(columns, 1), 1)
    for ((pane, ix) <- dtree.nodeChildren.map(mkTree(_, selected)).zipWithIndex) {
      treeGrid.add(pane, ix, 1)
    }

    // Make the second row take up all the space if possible.
    for (_ <- 1 to Math.max(columns, 1)) {
      val cc = new ColumnConstraints {
        hgrow = Priority.Always
        fillWidth = true
      }

      treeGrid.columnConstraints.add(cc)
    }

    val unfolded = BooleanProperty(true)
    folds.prepend(unfolded)

    // Get all the children to hide if this node is folded.
    treeGrid.children.filterNot(_ == rootNode.delegate).foreach { node =>
      node.visible <== unfolded
      node.managed <== unfolded
    }

    // Allows the use of right click / MOUSE2 to hide the children of the right-clicked node.
    val foldHandler: EventHandler[MouseEvent] = (event: MouseEvent) => {
      if (event.getButton == MouseButton.Secondary.delegate) {
        unfolded() = !unfolded()
      }
    }

    // Selects this current node for detailed display, defined in mkDTreeRect.
    val selectHandler = rootNode.onMouseClicked()

    // Ensures that the mouse click event can go to both the select and fold handlers.
    rootNode.onMouseClicked = event => {
      selectHandler.handle(event)
      foldHandler.handle(event)
    }

    // don't ask... apparently Scala 3 thinks this type is different...
    val rootBorder: ObjectBinding[layout.Border] = when(unfolded) choose Border.Empty otherwise simpleBorder(
      BorderStrokeStyle.Dotted
    )
    rootNode.border <== rootBorder

    // Check if the renamed parser matches its internal name.
    // If it does, return as usual.
    // Otherwise, make a dotted box around it with the renamed name.
    if (dtree.parserName == dtree.internalName) treeGrid
    else {
      // This is the inner box with the dotted border
      val innerBox = new HBox {
        padding = simpleInsets(2)
        alignmentInParent = Pos.Center
        alignment = Pos.Center
        border = simpleBorder(BorderStrokeStyle.Dashed)
        background = DefaultBackground
      }

      innerBox.children.add(treeGrid)

      // Wrap the inner box in another pane
      val spacer = new HBox {
        padding = simpleInsets(1)
        alignment = Pos.Center
        alignmentInParent = Pos.Center
        background = DefaultBackground
      }

      spacer.children.add(innerBox)

      // Name in a white box.
      val name = new Text {
        text = dtree.parserName
        font = defaultFont(1.5, FontWeight.Black)
        alignmentInParent = Pos.Center
      }

      val panel = new Pane {
        alignmentInParent = Pos.Center
      }

      val halfX = Bindings.createDoubleBinding(
        () => panel.layoutBounds().maxX / 2,
        panel.layoutBounds
      )

      val whiteBox = new StackPane {
        alignment = Pos.Center
        alignmentInParent = Pos.TopCenter
        background = DefaultBackground
        padding = Insets(0, relativeSize(1), 0, relativeSize(1))

        private val xbind = Bindings.createDoubleBinding(
          () => halfX().doubleValue() - this.layoutBounds().maxX / 2,
          this.layoutBounds,
          halfX
        )

        translateX <== xbind
      }

      whiteBox.children.add(name)

      // Compensate for the white box for the inner panel:
      innerBox.minWidth <== Bindings.createDoubleBinding(
        () => Math.max(whiteBox.width(), name.layoutBounds().getWidth) + relativeSize(2),
        whiteBox.width,
        name.layoutBounds
      )

      // The actual panel.
      panel.children.add(spacer)
      panel.children.add(whiteBox)

      val nameUnfolded = BooleanProperty(true)
      folds.prepend(nameUnfolded)

      spacer.visible <== nameUnfolded
      spacer.managed <== nameUnfolded

      whiteBox.onMouseClicked = event => {
        if (event.getButton == MouseButton.Secondary.delegate) {
          nameUnfolded() = !nameUnfolded()
        }
      }

      // don't ask... apparently Scala 3 thinks this type is different...
      val whiteBoxBorder: ObjectBinding[layout.Border] = when(nameUnfolded) choose Border.Empty otherwise simpleBorder(
        BorderStrokeStyle.Dotted
      )
      whiteBox.border <== whiteBoxBorder

      panel
    }
  }
}
