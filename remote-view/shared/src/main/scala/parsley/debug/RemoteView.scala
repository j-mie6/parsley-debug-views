/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug

// import scala.collection.Map

import parsley.debug.DebugView

import sttp.client3._

/** The RemoteView HTTP module allows the parsley debug tree to be passed off to a server through a specified port on
  * local host. This enables all of the debug tree parsing, serving and graphics to be done in a separate process, and
  * served reactively to a client.
  * 
  * This module is part of the Debugging Interactively in parsLey Library (DILL) project,
  * (https://github.com/j-mie6/parsley-debug-app).
  * 
  * RemoteView uses the STTP library to create HTTP requests to a port on localhost.
  */
sealed trait RemoteView extends DebugView.Reusable {
  implicit protected val port: Integer

  override private [debug] def render(input: => String, tree: => DebugTree): Unit = {
    // Create localhost endpoint using port
    val endPoint: String = "http://127.0.0.1:" + port.toString() + "/remote"

    // Transform debug tree to string here
    val possibleChildNumber = tree.childNumber.map(", " + _.toString).getOrElse("")
    val hasSuccess          = tree.parseResults.exists(_.success)
    val debugTree = s"DebugTree { name: ${tree.parserName} (${tree.internalName}$possibleChildNumber), success: $hasSuccess }"

    // Format payload as JSON
    val payload: String = "{\"input\": \"" + input + "\", \"tree\": \"" + debugTree + "\"}"

    println("Sending Debug Tree to Server")

    // Send POST
    val backend = HttpURLConnectionBackend()
    val r = basicRequest
      .header("User-Agent", "remoteView")
      .contentType("application/json")
      .body(payload)
      .post(uri"$endPoint")
      .send(backend)

    println("Done!")
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
