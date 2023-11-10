# Parsley Debugger Frontends

[Parsley](https://github.com/j-mie6/Parsley) is a fast parser-combinator library for Scala based on
Haskell's [Parsec](https://hackage.haskell.org/package/parsec).
This is a set of frontends for the debugger extension I am making for Parsley, found [here](https://github.com/MF42-DZH/parsley/tree/dev).

## Usage

Currently, until the `dev` branch containing my debugger extension passes all checks and is approved for merge, there is no way to use this other than locally publishing my fork of Parsley, and this repository (or just one of the frontends).

Each sub-project contains one frontend, currently:
- `con_ui` (@ `con-ui`): A console pretty-printer, `ConsolePrettyPrinter`.
- `sfx_ui` (@ `sfx-ui`): A [ScalaFX](https://www.scalafx.org/)-powered interactive GUI for exploring parser execution trees, `FxGUI`.
- `json_info` (@ `json-info`): A JSON string generator, `JsonFormatter` and `JsonStringFormatter`.

After adding one of these projects as a dependency, use one of the attaching combinators in `parsley.debugger.combinators` to make a parser automatically call the attached frontend in order to process it. You can find the frontends within the package `parsley.debugger.frontend`.
