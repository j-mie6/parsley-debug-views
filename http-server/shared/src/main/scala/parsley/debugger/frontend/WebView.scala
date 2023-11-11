/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import scala.xml.{PrettyPrinter, XML}

import cats._
import cats.effect._
import cats.effect.unsafe.IORuntime
import cats.implicits._
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.QueryParamDecoder._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.ember.server._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.ToHTML._
import parsley.debugger.frontend.internal.Styles

/** A frontend that uses `http4s` and the Ember server to provide an interactive web frontend for debugging parsers.
  * This is most useful for remote debugging of one's parsers.
  *
  * TODO: Figure out how to make this reusable.
  */
final class WebView private[frontend] (host: Hostname, port: Port) extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit = ???
}

object WebView {
  def apply(host: Hostname = host"localhost", port: Port = port"8080"): WebView =
    new WebView(host, port)
}

/** A raw HTML formatter for debug trees. */
final class HtmlFormatter private[frontend] (cont: String => Unit, spaces: Int) extends StatelessFrontend {
  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    // 640 is a sensible line length. Ideally we want an infinite line length limit.
    val printer = new PrettyPrinter(640, spaces)
    val sb      = new StringBuilder()

    val page =
      <html>
        <head>
          <title>Parsley Web Frontend</title>
          <style type="text/css">
            {Styles.primaryStylesheet}
          </style>
        </head>

        <body>
          <h1>Input</h1>
          <p class="large">{s"\"$input\""}</p>
          <hr />
          <h1>Parse Tree</h1>
          {tree.toHTML}
        </body>
      </html>

    printer.format(page, sb)

    // I have no idea how to get literal ampersands.
    cont("<!DOCTYPE html>\n" + sb.toString().replace(ampSeq, "&"))
  }
}

object HtmlFormatter {
  def apply(cont: String => Unit, spaces: Int = 2): HtmlFormatter = new HtmlFormatter(cont, spaces)
}
