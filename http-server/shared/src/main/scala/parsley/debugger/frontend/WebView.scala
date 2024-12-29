/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.frontend

import scala.collection.mutable
import scala.xml.{Node, PrettyPrinter}

import cats.*
import cats.data.Validated
import cats.effect.*
import cats.effect.unsafe.IORuntime
import cats.implicits.*
import com.comcast.ip4s.*
import fs2.compression.Compression
import fs2.io.compression._
import fs2.io.net.Network
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.server.Server
import org.http4s.server.middleware.GZip
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import org.typelevel.log4cats.syntax.*
import org.typelevel.scalaccompat.annotation.unused
import parsley.debugger.DebugTree
import parsley.debugger.frontend.internal.ToHTML.*
import parsley.debugger.frontend.internal.Styles

/** A frontend that uses `http4s` and the Ember server to provide an interactive web frontend for debugging parsers.
  * This is most useful for remote debugging of one's parsers.
  *
  * This class can only be created in the context of a `cats` monad that supports impure actions encoded as values...
  * most likely `IO`.
  *
  * `cont` will be called once when the server begins. Pass the server initialiser computation to your `cats` runtime.
  *
  * It is recommended that all memory-heavy types (e.g. closures) are not stored explicitly. Consult the documentation
  * on attaching debuggers to find out how to prevent that.
  *
  * '''Warning''': large tree outputs from debugged parsers may crash web browsers' rendering systems.
  */
sealed class WebView[F[_]: Logger: Async: Network: Compression, G] private[frontend] (
  cont: F[Resource[F, Server]] => G,
  host: Hostname,
  port: Port
) extends ReusableFrontend {
  // Seen trees. We'll use this to create links to previously-seen trees.
  private val seen: mutable.ListBuffer[(String, DebugTree)] = new mutable.ListBuffer()
  private val jsonCache: mutable.HashMap[Int, String]       = new mutable.HashMap()
  private val prettyJsonCache: mutable.HashMap[Int, String] = new mutable.HashMap()
  private val htmlCache: mutable.HashMap[Int, String]       = new mutable.HashMap()
  private val jsCache: mutable.HashMap[Int, String]         = new mutable.HashMap()
  private var started: Boolean = false

  // Download query matcher
  private object TreeMatcher extends ValidatingQueryParamDecoderMatcher[Int]("tree")

  private object PrettyMatcher extends FlagQueryParamMatcher("pretty")
  private object JsMatcher extends FlagQueryParamMatcher("js")

  // And here is where we will setup and create the server
  def start(): F[Resource[F, Server]] = {
    val routes: HttpRoutes[F] = {
      val dsl = Http4sDsl[F]
      import dsl.*

      GZip(
        HttpRoutes.of[F] {
          case GET -> Root / "download" :? TreeMatcher(index) +& PrettyMatcher(pretty) =>
            index match {
              case Validated.Valid(idx) =>
                val ix = idx - 1

                var sln: Int = 0
                var six: Option[(String, DebugTree)] = None

                seen.synchronized {
                  sln = seen.length
                  six = if (ix >= 0 && ix < sln) Some(seen(ix)) else None
                }

                if (six.isEmpty) {
                  NotFound(s"Index ${ix + 1} out of bounds.")
                } else {
                  var result = ""

                  six.foreach { case (inp, tree) =>
                    val cache = if (pretty) prettyJsonCache else jsonCache

                    cache.synchronized {
                      cache.get(ix) match {
                        case Some(json) => result = json
                        case None       =>
                          JsonStringFormatter(
                            r => {
                              result = r
                              cache(ix) = r
                            },
                            pretty = pretty
                          ).process(inp, tree)
                      }
                    }
                  }

                  Ok(result).map(_.putHeaders(`Content-Type`.parse("text/json")))
                }
              case Validated.Invalid(e) => BadRequest(s"Invalid parse for index: $e")
            }
          case GET -> Root / "view" :? TreeMatcher(index) +& JsMatcher(js)             =>
            index match {
              case Validated.Valid(idx) =>
                val ix = idx - 1

                var sln: Int = 0
                var six: Option[(String, DebugTree)] = None

                seen.synchronized {
                  sln = seen.length
                  six = if (ix >= 0 && ix < sln) Some(seen(ix)) else None
                }

                if (!js && six.isEmpty) {
                  NotFound(s"Index ${ix + 1} out of bounds.")
                } else if (js) {
                  jsCache.synchronized {
                    Ok(jsCache(ix)).map(_.putHeaders(`Content-Type`.parse("application/javascript")))
                  }
                } else {
                  // format: off
                  lazy val additions = List(
                      <hr />,
                      <p class="large">
                        <a href={s"/download?tree=$idx"}>Download this debug output as JSON</a>
                        <br />
                        <a href={s"/download?tree=$idx${ampSeq}pretty"}>(Prettified JSON)</a>
                      </p>,
                      <hr />,
                      <h1>Parse Trees</h1>,
                      <div class="toc large">
                        {seen.indices.map { ix =>
                        <a href={"/view?tree=" + (ix + 1).toString}>{ix + 1}</a>
                        }}
                      </div>)
                  // format: on

                  var result = ""

                  six.foreach { case (inp, tree) =>
                    htmlCache.synchronized {
                      jsCache.synchronized {
                        htmlCache.get(ix) match {
                          case Some(html) => result = html
                          case None       =>
                            HtmlFormatter(
                              r => {
                                result = r
                                htmlCache(ix) = r
                              },
                              j => {
                                jsCache(ix) = j
                              },
                              spaces = None,
                              treeNum = idx,
                              additions = additions
                            ).process(inp, tree)
                        }
                      }
                    }
                  }

                  Ok(result).map(_.putHeaders(`Content-Type`.parse("text/html")))
                }
              case Validated.Invalid(e) => BadRequest(s"Invalid parse for index: $e")
            }
          case _ => NotFound("Debug tree at that index not found.")
        }
      )
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
      _   <- info"Find your trees (as you process them) at $host:$port/view?tree=[index of tree (starting at 1)]."
    } yield srv
  }

  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    // The trees will be listed in index order.
    seen.synchronized {
      seen.append((input, tree))
    }

    // Start the server if it has not.
    if (!started) {
      started = true
      val _ = cont(start()): @unused
    }
  }
}

object WebView {
  def apply[F[_]: Logger: Async: Network: Compression, G](
    cont: F[Resource[F, Server]] => G,
    host: Hostname = host"localhost",
    port: Port = port"8080"
  ): WebView[F, G] =
    new WebView(cont, host, port) // "pure", yeah right.
}

/** A version of [[WebView]] for those who don't care about `cats` and just want something that works. The only problem
  * is that this may not work on all platforms.
  *
  * @note
  *   Depending on platform, you may want to keep the application from terminating should you want to keep the server
  *   running after your parsers have run.
  */
object WebViewUnsafeIO {
  implicit private val defaultIoRuntime: IORuntime = IORuntime.global

  def make(
    host: Hostname = host"localhost",
    port: Port = port"8080",
    logger: Logger[IO] = NoOpLogger[IO]
  ): WebView[IO, Unit] = {
    implicit val log: Logger[IO] = logger

    new WebView[IO, Unit](
      serverStart => {
        (for {
          serv <- serverStart
          res  <- serv.useForever.as(())
        } yield res).unsafeRunAsync(_ => ())
      },
      host,
      port
    )
  }
}

/** A raw HTML formatter for debug trees. If you don't want or need pretty-printing, pass `None` into `spaces`. Separate
  * continuation parameters are provided for the HTML and the JS.
  */
final private[parsley] class HtmlFormatter private[frontend] (
  contHTML: String => Unit,
  contJS: String => Unit,
  spaces: Option[Int],
  treeNum: Int,
  additions: Iterable[Node]
) extends ReusableFrontend {
  implicit private class Sanitize(s: String) {
    def sanitizeNewlines: String = s.replace("\r", "").replace("\n", nlSeq)

    def amp: String = s.replace(ampSeq, "&")

    def nl: String = s.replace(nlSeq, "<br />")
  }

  private lazy val style: String = dev.i10416.CSSMinifier.run(Styles.primaryStylesheet)

  override protected def processImpl(input: => String, tree: => DebugTree): Unit = {
    implicit val funcTable: mutable.Buffer[String] = mutable.ListBuffer()

    // format: off
    val page =
      <html>
        <head>
          <title>Parsley Web Frontend</title>
          <style type="text/css">
            ---[STYLE]---
          </style>
        </head>

        <body>
          <h1>Input</h1>
          <p class="large">{s"\"${input.sanitizeNewlines}\""}</p>
          <hr />
          <h1>Output</h1>
          <br />
          <p class="large">
            {tree.parseResults.flatMap(_.result) match {
              case Some(ans) => ans.toString
              case None      => "[N/A]"
            }}
          </p>
          <hr/>
          <h1>Parse Tree</h1>
          <button class="folds-btn" onclick="unfold_all()">
            Unfold All Children [!]
          </button>

          <button class="folds-btn" onclick="fold_all()">
            Fold All Children [!]
          </button>

          {tree.toHTML}

          {additions}

          <script src={s"view?tree=$treeNum&js"}></script>
        </body>
      </html>
    // format: on

    def script(): String = {
      ("""var os = {};
        |var fs = {};
        |var fk = -1;
        |var ss = {};
        |""".stripMargin + funcTable.mkString(start = "", sep = "\n", end = "\n") +
        """fk = Object.keys(ss).sort()[0];
          |
          |function unfold_all() {
          |  if (confirm("Are you sure you want to unfold all the trees? This may crash your browser if the tree is very large!")) {
          |    Object.keys(os).forEach((k) => load_child(k)(undefined));
          |  }
          |}
          |
          |function fold_all() {
          |  if (confirm("Are you sure you want to fold all the trees? You will lose your unfolding progress!")) {
          |    unload_children(fk)(undefined);
          |  }
          |}
          |
          |function asb(id, fun) {
          |  if (!ss[id]) ss[id] = [];
          |  ss[id].push(fun);
          |}
          |
          |function unload_children(uuid) {
          |  var evh = (e) => {
          |    ss[uuid].forEach((f) => f());
          |  };
          |
          |  return evh;
          |}
          |
          |function lc(uuid) {
          |  var evh = (e) => {
          |    let target = document.getElementById("child_" + uuid);
          |
          |    if (target && target.firstElementChild && target.firstElementChild.className.indexOf("unloaded") !== 1) {
          |      target.innerHTML = os[uuid][0];
          |
          |      if (target.hasAttribute("onclick")) {
          |        target.removeAttribute("onclick");
          |        target.addEventListener("click", lc(uuid), false);
          |      }
          |
          |      let parent = document.getElementById("parent_" + os[uuid][1]);
          |      if (parent) parent.addEventListener("click", unload_children(os[uuid][1]), false);
          |    } else if (target) {
          |      target.innerHTML = fs[uuid];
          |    }
          |
          |    var event = e ? e : window.event;
          |    event.cancelBubble = true;
          |    if (event.stopPropagation) event.stopPropagation();
          |    if (event.stopImmediatePropagation) event.stopImmediatePropagation();
          |  };
          |
          |  return evh;
          |}
          |
          |function unc(uuid) {
          |  let target = document.getElementById("child_" + uuid);
          |  if (target) target.innerHTML = fs[uuid];
          |}
          |""".stripMargin).amp.nl
        .replaceAll("\\n\\s+(\\S)", "$1")
        .replace(";\n", ";")
        .replace(" = ", "=")
    }

    spaces match {
      case Some(spc) =>
        // 800 is a sensible line length. Ideally we want an infinite line length limit.
        val printer = new PrettyPrinter(800, spc)
        val sb      = new StringBuilder()

        printer.format(page, sb)

        // I have no idea how to get literal ampersands.
        contHTML(
          "<!DOCTYPE html>\n" + sb
            .toString()
            .amp
            .nl
            .replace("---[STYLE]---", style)
        )
      case None      =>
        contHTML(
          "<!DOCTYPE html>\n" + page
            .toString()
            .amp
            .nl
            .replace("---[STYLE]---", style)
        )
    }

    contJS(script())
  }
}

private[parsley] object HtmlFormatter {
  def apply(
    contHTML: String => Unit,
    contJS: String => Unit,
    spaces: Option[Int] = Some(2),
    treeNum: Int,
    additions: Iterable[Node] = Nil
  ): HtmlFormatter =
    new HtmlFormatter(contHTML, contJS, spaces, treeNum, additions)
}
