/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug

import scala.concurrent.duration.*
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import sttp.client3.*
import sttp.model.Uri

import parsley.debug.internal.DebugTreeSerialiser

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
sealed trait RemoteView extends DebugView.Reusable with DebugView.Pauseable {
  protected val port: Int
  protected val address: String

  // Printing helpers
  private [debug] final val TextToRed: String    = "\u001b[31m"
  private [debug] final val TextToNormal: String = "\u001b[0m"
  
  // Request Timeouts
  private [debug] final val ConnectionTimeout: FiniteDuration = 30.second
  private [debug] final val ResponseTimeout: FiniteDuration = 10.second
  private [debug] final val BreakpointTimeout: FiniteDuration = 30.minute

  // Endpoint for post request
  private [debug] final lazy val endPoint: Uri = uri"http://$address:$port/api/remote/tree"

  /**
   * Send the debug tree and input to the port and address specified in the 
   * object construction. 
   *
   * @param input The input source.
   * @param tree The debug tree.
   */
  override private [debug] def render(input: => String, tree: => DebugTree): Unit = 
    renderWithTimeout(input, tree, ResponseTimeout)

  override private [debug] def renderWait(input: => String, tree: => DebugTree): Int = {
    renderWithTimeout(input, tree, BreakpointTimeout)
    0
  }

  private [debug] def renderWithTimeout(input: => String, tree: => DebugTree, timeout: FiniteDuration): Unit = {
    // JSON formatted payload for post request
    val payload: String = DebugTreeSerialiser.toJSON(input, tree)
    
    // Send POST
    println("Sending Debug Tree to Server")
    
    val backend = TryHttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(ConnectionTimeout)
    )
    
    val response: Try[Response[Either[String,String]]] = basicRequest
      .readTimeout(timeout)
      .header("User-Agent", "remoteView")
      .contentType("application/json")
      .body(payload)
      .post(endPoint)
      .send(backend)

    response match {
      case Failure(exception) => println(s"${TextToRed}Remote View request failed! Please validate address ($address) and port number ($port) and make sure the remote view app is running.${TextToNormal}\n\tError : ${exception.toString}")
      case Success(res) => res.body match {
        // Left indicates the request is successful, but the response code was not 2xx.
        case Left(errorMessage) => println(s"${TextToRed}Request Failed with message : $errorMessage, and status code : ${res.code}${TextToNormal}")
        // Right indicates a successful request with 2xx response code.
        case Right(body) => println(s"Request successful with message : $body")
      }
    }
  }
}

object RemoteView extends DebugView.Reusable with RemoteView {
    // Default port uses HTTP port and local host
    override protected val port: Int = 80
    override protected val address: String = "127.0.0.1"
    
    private final val MinimalIpLength: Int = "0.0.0.0".length
    private final val MaximalIpLength: Int = "255.255.255.255".length
    
    private final val MaxUserPort: Integer = 0xFFFF

    /** Do some basic validations for a given IP address. */
    private def checkIp(address: String): Boolean = {
      val addrLenValid: Boolean = address.length >= MinimalIpLength && address.length <= MaximalIpLength
      val addrDotValid: Boolean = address.count(_ == '.') == 3
      
      // Check that every number is a number
      val numberStrings: Array[String] = address.split('.')
      val addrNumValid: Boolean = numberStrings.forall((number: String) => number.length > 0 && {
          Try(number.toInt).toOption match {
            case None => false
            case Some(number) => number >= 0x0 && number <= 0xFF
          }
        }
      )

      addrLenValid && addrDotValid && addrNumValid 
    }

    /** Create a new instance of [[RemoteView]] with a given custom port. */
    def apply(userPort: Integer = port, userAddress: String = address): RemoteView = new RemoteView {
      require(userPort <= MaxUserPort, s"Remote View port invalid : $userPort > $MaxUserPort")
      require(checkIp(userAddress), s"Remote View address invalid : $userAddress")

      override protected val port = userPort
      override protected val address = userAddress
    }
}

/** Helper object for connecting to the DILL backend. */
object DillRemoteView extends DebugView.Reusable with RemoteView {
  // Default endpoint for DILL backend is port 17484 ("DL") on localhost
  override protected val port: Int = 17484
  override protected val address: String = "127.0.0.1"

  /** Create a new instance of [[RemoteView]] with default ports for the DILL backend server. */
  def apply(userAddress: String = address): RemoteView = RemoteView(port, userAddress)
}
