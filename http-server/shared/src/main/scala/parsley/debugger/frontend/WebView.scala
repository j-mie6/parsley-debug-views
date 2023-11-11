/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import scala.collection.mutable
import scala.xml.{Node, PrettyPrinter}

import cats._
import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s._
import org.http4s.dsl._
import org.http4s.ember.server._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._
import org.typelevel.scalaccompat.annotation.unused
import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.ToHTML._
import parsley.debugger.frontend.internal.Styles

/** A frontend that uses `http4s` and the Ember server to provide an interactive web frontend for debugging parsers.
  * This is most useful for remote debugging of one's parsers.
  *
  * This class can only be created in the context of a `cats` monad that supports impure actions encoded as values...
  * most likely `IO`.
  *
  * `cont` will be called once when the server begins. Pass the server initialiser computation to your `cats` runtime.
  */
final class WebView[F[_]: Logger: Async: Network, G] private[frontend] (
  cont: F[Resource[F, Server]] => G,
  host: Hostname,
  port: Port
) extends StatelessFrontend {
  // Seen trees. We'll use this to create links to previously-seen trees.
  private val seen: mutable.ListBuffer[(String, DebugTree)] = new mutable.ListBuffer()
  private var started: Boolean = false

  // And here is where we will setup and create the server
  def start(): F[Resource[F, Server]] = {
    val routes: HttpRoutes[F] = {
      val dsl = Http4sDsl[F]
      import dsl._

      HttpRoutes.of[F] {
        case GET -> Root / ix =>
          ix.toIntOption match {
            case Some(index) =>
              val ix = index - 1
              if (ix < 0) {
                BadRequest("Zero or negative index given.")
              } else if (ix >= seen.length) {
                NotFound("Index out of bounds.")
              } else {
                // format: off
                val additions = List(<hr />, <h1>Parses</h1>, <div class="toc large">
                  {seen.indices.map { ix =>
                    <a href={"/" + (ix + 1).toString}>{ix + 1}</a>
                  }}
                </div>)
                // format: on

                var result = ""
                HtmlFormatter(r => { result = r }, 0, additions).process(seen(ix)._1, seen(ix)._2)

                Ok(result).map(_.putHeaders(`Content-Type`.parse("text/html")))
              }
            case None        => BadRequest("Non-integer index given.")
          }
        case _                => NotFound("Debug tree at that index not found.")
      }
    }

    val app: HttpApp[F] = routes.orNotFound

    for {
      _   <- info"Starting debug web view server at $host:$port."
      srv <- implicitly[Monad[F]].pure {
        EmberServerBuilder
          .default[F]
          .withPort(port)
          .withHost(host)
          .withHttpApp(app)
          .build
      }
      _   <- info"Find your trees (as you process them) at $host:$port/[index of tree (starting at 1)]."
    } yield srv
  }

  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    // The trees will be listed in index order.
    seen.append((input, tree))

    // Start the server if it has not.
    if (!started) {
      started = true
      val _ = cont(start()): @unused
    }
  }
}

object WebView {
  def apply[F[_]: Logger: Async: Network, G](
    cont: F[Resource[F, Server]] => G,
    host: Hostname = host"localhost",
    port: Port = port"8080"
  ): WebView[F, G] =
    new WebView(cont, host, port) // "pure", yeah right.
}

/** A raw HTML formatter for debug trees. */
final class HtmlFormatter private[frontend] (cont: String => Unit, spaces: Int, additions: Iterable[Node])
  extends StatelessFrontend {
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
          {additions}
        </body>
      </html>

    printer.format(page, sb)

    // I have no idea how to get literal ampersands.
    cont("<!DOCTYPE html>\n" + sb.toString().replace(ampSeq, "&"))
  }
}

object HtmlFormatter {
  def apply(cont: String => Unit, spaces: Int = 2, additions: Iterable[Node] = Nil): HtmlFormatter =
    new HtmlFormatter(cont, spaces, additions)
}
