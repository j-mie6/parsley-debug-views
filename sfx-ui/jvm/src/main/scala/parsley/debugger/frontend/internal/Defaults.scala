/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend.internal

import scalafx.geometry.Insets
import scalafx.scene.layout.{
  Background,
  BackgroundFill,
  Border,
  BorderStroke,
  BorderStrokeStyle,
  BorderWidths,
  CornerRadii
}
import scalafx.scene.paint.{Color, Paint}
import scalafx.scene.text.{Font, FontWeight}

// This will contain common constants and methods.
private[frontend] object Defaults {
  // Default background; #f4f4f4.
  val DefaultBGColour: Color = Color.rgb(244, 244, 244)

  val DefaultBackground: Background = new Background(
    Array(
      new BackgroundFill(
        DefaultBGColour,
        CornerRadii.Empty,
        Insets.Empty
      )
    )
  )

  val SuccessColour: Color = Color.LightGreen

  val FailureColour: Color = Color.rgb(201, 115, 109)

  def simpleBackground(fill: Paint): Background = new Background(
    Array(
      new BackgroundFill(
        fill,
        CornerRadii.Empty,
        Insets.Empty
      )
    )
  )

  def simpleBorder(
    style: BorderStrokeStyle
  )(implicit globalMult: Double): Border = new Border(
    new BorderStroke(
      Color.Black,
      style,
      CornerRadii.Empty,
      new BorderWidths(relativeSize(0.2))
    )
  )

  def simpleInsets(multiplier: Double)(implicit globalMult: Double): Insets =
    Insets(relativeSize(multiplier))

  // For easy use, all sizes will be relative to the default font size.
  def relativeSize(multiplier: Double)(implicit globalMult: Double): Double =
    Font.default.size * multiplier * globalMult

  def defaultFont(
    multiplier: Double,
    weight: FontWeight = FontWeight.Normal
  )(implicit globalMult: Double): Font =
    Font(Font.default.family, weight, relativeSize(multiplier))

  def monoFont(
    multiplier: Double,
    weight: FontWeight = FontWeight.Normal
  )(implicit globalMult: Double): Font =
    Font("monospace", weight, relativeSize(multiplier))
}
