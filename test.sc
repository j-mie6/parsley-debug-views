//> using dep com.github.j-mie6::parsley:5.0.0-M10
//> using dep com.github.j-mie6::parsley-debug:5.0.0-M10
//> using jar com.github.j-mie6::parsley-debug-remote:0.1-0954e67-20250114T172800Z-SNAPSHOT

import parsley.quick.*
import parsley.debug.combinator.*
import parsley.debug.RemoteView

char('a').attach(RemoteView(port = 80)).parse("a")
