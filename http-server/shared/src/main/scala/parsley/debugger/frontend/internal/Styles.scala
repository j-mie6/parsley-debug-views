package parsley.debugger.frontend.internal

private[frontend] object Styles {
  val primaryStylesheet: String = """
      |* {
      |  border-spacing: 0 !important;
      |}
      |
      |body {
      |  background-color: #f0f0f0;
      |  font-family: monospace;
      |}
      |
      |th {
      |  text-align: right;
      |}
      |
      |td {
      |  padding: 0;
      |  margin: 0;
      |}
      |
      |.parser {
      |  margin: 0;
      |  padding: 0;
      |}
      |
      |.nickname {
      |  position: absolute;
      |  display: block;
      |
      |  align-self: center;
      |  text-align: center;
      |
      |  margin-top: -2.3em;
      |  padding-left: 1.25em;
      |  padding-right: 1.25em;
      |  margin-left: 0.35em;
      |
      |  background-color: #f0f0f0;
      |  font-weight: bold;
      |}
      |
      |.dotted {
      |  border: 2px dashed black;
      |  padding: 1em;
      |  margin: 0.5em;
      |}
      |
      |.info {
      |  position: absolute;
      |  display: block;
      |  visibility: hidden;
      |
      |  background-color: #ffffff;
      |  border: 2px solid;
      |  border-color: #212223;
      |
      |  padding: 0.7em;
      |  left: 50%;
      |  transform: translate(-50%, -0.2em);
      |
      |  z-index: 999 !important;
      |}
      |
      |.info * {
      |  vertical-align: baseline;
      |}
      |
      |.attempt {
      |  display: block;
      |  margin: 0.5em;
      |  padding-top: 0.125em;
      |  padding-bottom: 0.125em;
      |  padding-left: 1em;
      |  padding-right: 1em;
      |}
      |
      |.attempt:hover {
      |  background-color: #ffff00 !important;
      |}
      |
      |.attempt:hover .info {
      |  position: absolute;
      |  visibility: visible;
      |  display: block;
      |}
      |
      |.children:last-child > .attempt:hover .info {
      |  display: none !important;
      |}
      |
      |.overview {
      |  width: 100%;
      |  text-align: center;
      |  font-weight: bold;
      |  line-height: 1.75;
      |}
      |
      |.success {
      |  background-color: #87ff87;
      |}
      |
      |.failure {
      |  background-color: #ff8787;
      |}
      |
      |.children {
      |  padding: 0;
      |}
      |
      |.children * {
      |  vertical-align: top;
      |}
      |
      |.children > tr {
      |  padding: 0;
      |}""".stripMargin
}
