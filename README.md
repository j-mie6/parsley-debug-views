# Parsley Debug-Views

[Parsley](https://github.com/j-mie6/Parsley) is a modern parser-combinator library for Scala.
The `parsley-debug` package allows us to debug parsers using a more comprehensive mechanism, though it
only supports printing out-of-the-box. This repo contains the implementations of different debug-views,
which can be used to visualise the results of a parse. This work was originally a Bachelors Project at
Imperial College London by Fawwaz Abdullah (@mf42-dzh).

## Usage

Each sub-project contains one view, currently:
- `parsley-debug-sfx` (@ `sfx-ui`): A [ScalaFX](https://www.scalafx.org/)-powered interactive GUI for exploring parser execution trees, `FxGUI`.
- `parsley-debug-json` (@ `json-info`): A JSON string generator, `JsonFormatter` and `JsonStringFormatter`.
<!-- - `parsley-debug-http` (@ `http-server`): A [http4s](https://http4s.org/) web server providing a semi-interactive parse tree viewer. The main class is `WebView`, but there is a helper object for people who are not interested in `cats` or `cats-effect`, `WebViewUnsafeIO`.-->

After adding one of these projects as a dependency, use one of the `attach` combinators in `parsley.debug.combinators` to make a parser render the debugging output with the given view. You can find the views within the package `parsley.debug`.
Currently, these views support:

| Version    | `parsley-debug` |
| ---------- | --------------- |
| `0.1.0-M1` | `5.0.0-M9`      |

## Supported Configurations
The different views work on different platforms:

### `PrintView` (`parsley-debug`)
| Version  | Scala (JDK8+)      | Scala.js (1.16+)   | Scala Native (0.5+) |
| -------- | ------------------ | ------------------ | ------------------- |
| 2.12     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |
| 2.13     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |
| 3.0      | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |

### `JsonFormatter` (`parsley-debug-json`)
| Version  | Scala (JDK8+)      | Scala.js (1.16+)   | Scala Native (0.5+) |
| -------- | ------------------ | ------------------ | ------------------- |
| 2.12     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |
| 2.13     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |
| 3.0      | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  |

<!--
### `WebView` (`parsley-debug-http`)
| Version  | Scala (JDK8+)      | Scala.js (1.16+)   | Scala Native (0.5+) |
| -------- | ------------------ | ------------------ | ------------------- |
| 2.12     | :x:                | :x:                | :x:                 |
| 2.13     | :heavy_check_mark: | :heavy_check_mark: | :x:                 |
| 3.0      | :heavy_check_mark: | :heavy_check_mark: | :x:                 |
-->

### `FxGUI` (`parsley-debug-sfx`)
| Version  | Scala (JDK8+)      | Scala.js (1.16+)   | Scala Native (0.5+) |
| -------- | ------------------ | ------------------ | ------------------- |
| 2.12     | :heavy_check_mark: | :x:                | :x:                 |
| 2.13     | :heavy_check_mark: | :x:                | :x:                 |
| 3.0      | :heavy_check_mark: | :x:                | :x:                 |

<!--Scala Native 0.5 support would be available for `parsley-debug-http` when `http4s`
has support. 2.12 support for all three new views will be supported in future too.-->

## Example

Assuming you have `parsley-debug-sfx` in your dependencies, this is a small example in debugging an arithmetic parser (with either `-experimental` for Scala 3 or `-Ymacro-annotations` for Scala 2.13):

```scala
import parsley.quick.*
import parsley.debug.combinator.*
import parsley.debug.FxGUI
import parsley.expr.{InfixL, Ops, precedence}

@parsley.debuggable
object Maths {
    val int: Parsley[BigInt] = satisfy(_.isDigit).foldLeft1(BigInt(0))((acc, c) => acc * 10 + c.asDigit)

    lazy val expr: Parsley[BigInt] = precedence[BigInt](int, char('(') ~> expr <~ char(')'))(
        Ops(InfixL)(char('*') as (_ * _), char('/') as (_ / _)),
        Ops(InfixL)(char('+') as (_ + _), char('-') as (_ - _))
    )

    lazy val prog: Parsley[List[BigInt]] = many(many(endOfLine) ~> expr)

    def main(args: Array[String]): Unit = {
        // Attach a graphical debugger to our main `prog` parser.
        val debugged = prog.attach(FxGUI)

        // Show the parse path for this complicated expression.
        // The print is to show the result of the parse.
        println(debugged.parse("(1+2)*(4-3)"))
    }
}
```

The UI is shown as follows:

![An interactive GUI window displaying the paths the parser has taken during execution.](/media/parse-arith.png?raw=true)

As `parsley-debug` itself has `PrintView`, then you can also easily switch over to using it from the ScalaFX GUI frontend. Simply change these lines:

```diff
- import parsley.debug.FxGUI
+ import parsley.debug.PrintView
...
- val debugged = prog.attach(FxGUI)
+ val debugged = prog.attach(PrintView)
```

And your output will be something as follows:

```
prog's parse tree for input:

(1+2)*(4-3)
[ prog (many) ]: ("{1}" [(1,1) -> (1,12)], Success - [ List(3) ])
|
+-[ ~>(1) ]: ("{1}" [(1,1) -> (1,12)], Success - [ 3 ])
| |
| +-[ many ]: ("" [(1,1) -> (1,1)], Success - [ List() ])
| | |
| | +-[ endOfLine ]: ("" [(1,1) -> (1,1)], Failure)
| |
| +-[ expr (infix.left1(1)) ]: ("{1}" [(1,1) -> (1,12)], Success - [ 3 ])
|   |
|   +-[ infix.left1(1) ]: ("{1}{2}{3}" [(1,1) -> (1,12)], Success - [ 3 ])
|   | |
|   | +-[ choice(1) ]: ("{1}" [(1,1) -> (1,6)], Success - [ 3 ])
|   | | |
[snip!]
```

And that is it!
