/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug

// import scala.collection.Map

import parsley.debug.DebugView

import sttp.client3.basicRequest
// import sttp.client3.circe.asJson
// import sttp.client3.circe._

/** The RemoteView HTTP module allows the parsley debug tree to be passed off to a server through a specified port on
  * local host. This enables all of the debug tree parsing, serving and graphics to be done in a separate process, and
  * served reactively to a client.
  * 
  * This module is part of the Debugging Interactively in parsLey Library (DILL) project,
  * (https://github.com/j-mie6/parsley-debug-app).
  * 
  * RemoteView uses the STTP library to create HTTP requests to a port on local host, and CIRCE to format the body of 
  * requests into JSON.
  */
sealed trait RemoteView extends DebugView.Reusable {
  implicit protected val port: Integer

  override private [debug] def render(input: => String, tree: => DebugTree): Unit = {
    val endPoint: sttp.model.Uri = sttp.model.Uri("http://localhost:" + port.toString() + "/remote")

    // val requestPayload = RequestPayload(Map[String, String]("input" -> input, "tree" -> tree))

    // val response: Identity[Response[Either[ResponseException[String, io.circe.Error], ResponsePayload]]] =
    val r = basicRequest
      .header("User-Agent", "remoteView")
      .contentType("application/json")
      .body("Hello, World!")
      // .body(asJson[RequestPayload])
      .post(endPoint)
  }
}

object RemoteView extends DebugView.Reusable with RemoteView {
    // Default port number for Tauri Remote Server
    override implicit val port = 5173

    /** Create a new instance of [[RemoteView]] with a given custom port. */
    def apply(user_port: Integer = port): RemoteView = new RemoteView {
        override implicit val port = user_port
    }
}
