/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug

import parsley.debug.internal.DebugTreeSerialiser

import sttp.client3.*

import scala.util.Try
import scala.concurrent.duration.*

/** The RemoteView HTTP module allows the parsley debug tree to be passed off to a server through a specified port on
  * local host (by default) or to a specified IP address. This enables all of the debug tree parsing, serving and 
  * graphics to be done in a separate process, and served reactively to a client.
  * 
  * This module is part of the Debugging Interactively in parsLey Library (DILL) project,
  * (https://github.com/j-mie6/parsley-debug-app).
  * 
  * RemoteView uses the STTP library to create HTTP requests to a specified IP address, over a specified port.
  * The request is formatted using the upickle JSON formatting library, it is being used over other
  * libraries like circe for its improved speed over large data structures.
  */
sealed trait RemoteView extends DebugView.Reusable {
  implicit protected val port: Integer
  implicit protected val address: String

  /**
    * Send the debug tree and input to the port and address specified in the 
    * object construction. 
    *
    * @param input The input source.
    * @param tree The debug tree.
    */
  override private [debug] def render(input: => String, tree: => DebugTree): Unit = {
    // Printing helpers
    val TEXT_TO_RED: String    = "\u001b[31m"
    val TEXT_TO_NORMAL: String = "\u001b[0m"

    val CONNECTION_TIMEOUT: FiniteDuration = 30.second
    val RESPONSE_TIMEOUT: FiniteDuration = 10.second

    // Create localhost endpoint using port
    val sb: StringBuilder = new StringBuilder("http://")
    sb.append(address)
    sb.append(':')
    sb.append(port.toString())
    sb.append("/remote")
    val endPoint: String = sb.result()

    // Format payload as JSON
    sb.clear()
    sb.append("{\"input\": \"")
    sb.append(input)
    sb.append("\", \"tree\": ")
    sb.append(DebugTreeSerialiser.toJSON(tree))
    sb.append("}")
    val payload: String = sb.result()

    println("Sending Debug Tree to Server")

    // Send POST
    try {
      val backend = HttpURLConnectionBackend(
        options = SttpBackendOptions.connectionTimeout(CONNECTION_TIMEOUT)
      )
      val r: Response[Either[String, String]] = basicRequest
        .readTimeout(RESPONSE_TIMEOUT)
        .header("User-Agent", "remoteView")
        .contentType("application/json")
        .body(payload)
        .post(uri"$endPoint")
        .send(backend)
        
        if (r.body.isLeft) {
          println(s"${TEXT_TO_RED}Request Failed with message : ${r.body.getOrElse("<no-response>")}${TEXT_TO_NORMAL}")
        } else {
          println(s"Response from server : ${r.body.getOrElse("<no-response>")}")
        }
    } catch {
      case _: Throwable => println(s"${TEXT_TO_RED}Remote View request failed! Please validate address ($address) and port number ($port).${TEXT_TO_NORMAL}")
    }

  }
}

object RemoteView extends DebugView.Reusable with RemoteView {
    // Default port uses HTTP port and local host
    override implicit val port = 80
    override implicit val address = "127.0.0.1"
    
    /** Do some basic validations for a given IP address. */
    private def checkIp(address: String): Boolean = {
      val minimalAddress: String = "0.0.0.0"
      val maximalAddress: String = "255.255.255.255"

      if (address.length() < minimalAddress.length() || address.length() > maximalAddress.length()) {
        return false
      }
      
      if (address.count(_ == '.') != 3) {
        return false
      }
      
      // Check that every number is a number
      val numberStrings: Array[String] = address.split('.')
      numberStrings.forall((number: String) => number.length() > 0 && Try(number.toInt).toOption.nonEmpty)
    }

    /** Create a new instance of [[RemoteView]] with a given custom port. */
    def apply(user_port: Integer = port, user_address: String = address): RemoteView = new RemoteView {
      private val MAX_USER_PORT: Integer = 65535
      assert(user_port <= MAX_USER_PORT, s"Remote View port invalid : $user_port > $MAX_USER_PORT")
      assert(checkIp(user_address), s"Remote View address invalid : $user_address")

      override implicit val port = user_port
      override implicit val address = user_address
    }
}

/** Helper object for connecting to the DILL backend. */
object DillRemoteView extends RemoteView {
  // Default endpoint for DILL backend is port 5173 on localhost
  override implicit protected val address: String = "127.0.0.1"
  override implicit protected val port: Integer = 5173

  /** Create a new instance of [[RemoteView]] with default ports for the DILL backend server. */
  def apply(): RemoteView = RemoteView(port, address)
}