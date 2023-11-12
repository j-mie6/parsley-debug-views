# Parsley Debugger Frontends

[Parsley](https://github.com/j-mie6/Parsley) is a fast parser-combinator library for Scala based on
Haskell's [Parsec](https://hackage.haskell.org/package/parsec).
This is a set of frontends for the debugger extension I am making for Parsley, found [here](https://github.com/MF42-DZH/parsley/tree/dev) for now.

## Usage

Currently, until the `dev` branch containing my debugger extension passes all checks and is approved for merge, there is no way to use this other than locally publishing my fork of Parsley, and this repository (or just one of the frontends).

Each sub-project contains one frontend, currently:
- `parsley-debug-console` (@ `con-ui`): A console pretty-printer, `ConsolePrettyPrinter`.
- `parsley-debug-sfx` (@ `sfx-ui`): A [ScalaFX](https://www.scalafx.org/)-powered interactive GUI for exploring parser execution trees, `FxGUI`.
- `parsley-debug-json` (@ `json-info`): A JSON string generator, `JsonFormatter` and `JsonStringFormatter`.

After adding one of these projects as a dependency, use one of the attaching combinators in `parsley.debugger.combinators` to make a parser automatically call the attached frontend in order to process it. You can find the frontends within the package `parsley.debugger.frontend`.

## Example

Assuming you have `parsley-debug-sfx` in your dependencies, this is a small example in debugging an arithmetic parser:

```
import parsley.Parsley
import parsley.character.{char, satisfy}
import parsley.combinator._
import parsley.debugger.combinator._
import parsley.debugger.frontend.FxGUI
import parsley.debugger.util.Collector
import parsley.expr.{InfixL, Ops, precedence}

object Maths {
  val int: Parsley[BigInt] =
    satisfy(_.isDigit)
      .foldLeft1(BigInt(0))((acc, c) => acc * 10 + c.asDigit)

  lazy val expr: Parsley[BigInt] =
    precedence[BigInt](
      int,
      char('(') ~> expr <~ char(')')
    )(
      Ops(InfixL)(
        char('*') #> (_ * _),
        char('/') #> (_ / _)
      ),
      Ops(InfixL)(
        char('+') #> (_ + _),
        char('-') #> (_ - _)
      )
    )

  lazy val prog: Parsley[List[BigInt]] =
    many(many(satisfy("\r\n".contains(_))) ~> expr)

  def main(args: Array[String]): Unit = {
    // Automatically grabs parser names from this Maths object.
    Collector.names(this)

    // Attach a graphical debugger to our main `prog` parser.
    val debugged = attachWithFrontend(prog, FxGUI())

    // Show the parse path for this complicated expression.
    // The print is to show the result of the parse.
    println(debugged.parse("(1+2)*(4-3)"))
  }
}
```

The UI is shown as follows:

![An interactive GUI window displaying the paths the parser has taken during execution.](/media/parse-arith.png?raw=true)

If for example you also had `parsley-debug-console` (the console pretty printer) as a dependency, then you can also easily switch over to using it from the ScalaFX GUI frontend. Simply change these lines:

```diff
- import parsley.debugger.frontend.FxGUI
+ import parsley.debugger.frontend.ConsolePrettyPrinter
...
- val debugged = attachWithFrontend(prog, FxGUI())
+ val debugged = attachWithFrontend(prog, ConsolePrettyPrinter())
```

And your output will be someting as follows:

```
prog's parse tree for input:

(1+2)*(4-3)


[ prog ]: ("(1+2)*(4-3)" [(1,1) -> (1,11)], Success - [ List(3) ])
|
+-[ ~> ]: ("(1+2)*(4-3)" [(1,1) -> (1,11)], Success - [ 3 ])
| |
| +-[ many ]: ("" [(1,1) -> (1,0)], Success - [ List() ])
| | |
| | +-[ satisfy ]: ("(" [(1,1) -> (1,1)], Failure)
| |
| +-[ expr ]: ("(1+2)*(4-3)" [(1,1) -> (1,11)], Success - [ 3 ])
|   |
|   +-[ left1 ]: ("(1+2)*(4-3)" [(1,1) -> (1,11)], Success - [ 3 ])
|   | |
|   | +-[ <|> ]: ("(1+2)" [(1,1) -> (1,5)], Success - [ 3 ])
|   | | |
[... snip!]
```

And that is it!
