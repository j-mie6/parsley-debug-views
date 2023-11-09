/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

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
package object internal {
  // Default background; #f4f4f4.
  private[frontend] val DefaultBGColour: Color = Color.rgb(244, 244, 244)

  private[frontend] val DefaultBackground: Background = new Background(
    Array(
      new BackgroundFill(
        DefaultBGColour,
        CornerRadii.Empty,
        Insets.Empty
      )
    )
  )

  private[frontend] val SuccessColour: Color = Color.LightGreen

  private[frontend] val FailureColour: Color = Color.rgb(201, 115, 109)

  private[frontend] def simpleBackground(fill: Paint): Background = new Background(
    Array(
      new BackgroundFill(
        fill,
        CornerRadii.Empty,
        Insets.Empty
      )
    )
  )

  private[frontend] def simpleBorder(
    style: BorderStrokeStyle
  )(implicit globalMult: Double): Border = new Border(
    new BorderStroke(
      Color.Black,
      style,
      CornerRadii.Empty,
      new BorderWidths(relativeSize(0.2))
    )
  )

  private[frontend] def simpleInsets(multiplier: Double)(implicit globalMult: Double): Insets =
    Insets(relativeSize(multiplier))

  // For easy use, all sizes will be relative to the default font size.
  private[frontend] def relativeSize(multiplier: Double)(implicit globalMult: Double): Double =
    Font.default.size * multiplier * globalMult

  private[frontend] def defaultFont(
    multiplier: Double,
    weight: FontWeight = FontWeight.Normal
  )(implicit globalMult: Double): Font =
    Font(Font.default.family, weight, relativeSize(multiplier))

  private[frontend] def monoFont(
    multiplier: Double,
    weight: FontWeight = FontWeight.Normal
  )(implicit globalMult: Double): Font =
    Font("monospace", weight, relativeSize(multiplier))
}
