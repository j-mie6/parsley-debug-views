//> using dep com.github.j-mie6::parsley:5.0.0-M10
//> using dep com.github.j-mie6::parsley-debug:5.0.0-M10
//> using jar /homes/rh1122/.ivy2/local/com.github.j-mie6/parsley-debug-remote_3/0.1-789ba97-20250116T163323Z-SNAPSHOT/jars/parsley-debug-remote_3.jar
//> using dep com.softwaremill.sttp.client3::core:3.10.2
//> using dep com.lihaoyi::upickle:4.1.0 

import parsley.quick.*
import parsley.syntax.character.{charLift, stringLift}
import parsley.debug.combinator.*

import parsley.debug.DillRemoteView

('h' ~> ("ello" | "i") ~> " world!").attach(DillRemoteView()).parse("hello world!")

// char('a').char('b').attach(DillRemoteView()).parse("ab")
