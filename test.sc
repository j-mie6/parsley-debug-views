//> using dep com.github.j-mie6::parsley:5.0.0-M10
//> using dep com.github.j-mie6::parsley-debug:5.0.0-M10
//> using jar /homes/rh1122/.ivy2/local/com.github.j-mie6/parsley-debug-remote_3/0.1-0924c11-20250114T191428Z-SNAPSHOT/jars/parsley-debug-remote_3.jar
//> using dep com.softwaremill.sttp.client3::core:3.10.2

import parsley.quick.*
import parsley.debug.combinator.*
import parsley.debug.RemoteView

char('a').attach(RemoteView(8000)).parse("a")
