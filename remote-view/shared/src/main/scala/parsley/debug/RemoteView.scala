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
import sttp.client3.upicklejson.*
import upickle.default as up

import parsley.debug.internal.{DebugTreeSerialiser, RemoteViewResponse, ParserInfoCollector}
import parsley.debug.RefCodec.CodedRef

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
sealed trait RemoteView extends DebugView.Reusable with DebugView.Pauseable with DebugView.Manageable {
  protected val port: Int
  protected val address: String

  // Identifies a session for the receiver
  private var sessionId: Int = -1
  
  // Printing helpers
  def colour(str: String, colour: String): String = s"$colour$str${Console.RESET}"
  
  // Request Timeouts
  private [debug] final val ConnectionTimeout = 30.second
  private [debug] final val ResponseTimeout   = 10.second
  private [debug] final val BreakpointTimeout = 30.minute
  
  // Endpoint for post request
  private [debug] final lazy val endPoint = uri"http://$address:$port/api/remote/tree"
  
  /**
  * Send the debug tree and input to the port and address specified in the 
  * object construction. 
  *
  * @param input The input source.
  * @param tree The debug tree.
  */
  override private [debug] def render(input: =>String, tree: =>DebugTree): Unit = {
    // Return value of the renderWithTimeout function not needed for a regular parse
    val _ = renderWithTimeout(input, tree, ResponseTimeout)
  }
  
  /**
  * Send the debug tree and input to the port and address specified in the
  * object construction.
  * 
  * This function will block and wait for a response from the remote view.
  * This is to allow breakpoints to halt Parsley parsing and wait for a 
  * number of breakpoints to skip.
  * 
  * The number of breakpoints to skip represents:
  *   n == 0  : Move through the current breakpoint and halt on the next.
  *   n >= 1  : Move through the current breakpoint and skip the next n breakpoints.
  *   n <= -1 : Stop the parser and exit the program.
  *
  * @param input The input source.
  * @param tree The debug tree.
  * 
  * @return The number of breakpoints to skip after this breakpoint exits.
  */
  override private [debug] def renderWait(input: =>String, tree: =>DebugTree): Int = {
    renderWithTimeout(input, tree, BreakpointTimeout, isDebuggable = true).getSkipsOrDefault
  }
  
  /**
  * Send the debug tree and input to the port and address specified in the
  * object construction. 
  * 
  * Wait for breakpoints to be skipped and references to be modified.
  *  
  * @param input The input source.
  * @param tree The debug tree.
  * @param refs Variable coded reference arguments encoded as tuples of Int address and String value 
  * 
  * @return The number of breakpoints to skip after this breakpoint exits.
  */
  override private [debug] def renderManage(input: =>String, tree: =>DebugTree, refs: CodedRef*): (Int, Seq[CodedRef]) = {
    val resp: Option[RemoteViewResponse] = renderWithTimeout(input, tree, BreakpointTimeout, isDebuggable = true, refs.toSeq)
    (resp.getSkipsOrDefault, resp.getNewRefsOrDefault)
  }
  
  private [debug] def renderWithTimeout(input: =>String, tree: =>DebugTree, timeout: FiniteDuration, isDebuggable: Boolean = false, refs: Seq[CodedRef] = Nil): Option[RemoteViewResponse] = {
    // JSON formatted payload for post request
    val payload: String = DebugTreeSerialiser.toJSON(input, tree, sessionId, ParserInfoCollector.info.toList, isDebuggable, refs)
    
    // Send POST
    println("Sending Debug Tree to Server...")
    if (isDebuggable) {
      if (refs.nonEmpty) print("\tManaging state.")
      println("\tWaiting for debugging input...")
    }

    
    // Implicit JSON deserialiser
    implicit val responsePayloadRW: up.ReadWriter[RemoteViewResponse] = up.macroRW[RemoteViewResponse]
    
    val backend = TryHttpURLConnectionBackend(
      options = SttpBackendOptions.connectionTimeout(ConnectionTimeout)
    )
    
    val response: Try[Response[Either[ResponseException[String, Exception], RemoteViewResponse]]] = basicRequest
      .readTimeout(timeout)
      .header("User-Agent", "remoteView")
      .contentType("application/json")
      .body(payload)
      .post(endPoint)
      .response(asJson[RemoteViewResponse])
      .send(backend)
    
    response match {
      // Failed to send POST request
      case Failure(exception) => {
        println(s"${colour("Remote View request failed! ", Console.RED)}" +
          s"Please validate address (${colour(address.toString, Console.YELLOW)}) and " +
          s"port number (${colour(port.toString, Console.YELLOW)}) and " +
          s"make sure the Remote View app is running.")
        
        println(s"\t${colour("Error:", Console.RED)} ${exception.toString}")
        None
      }
      
      // POST request was successful
      case Success(res) => res.body match {
        // Response was failed response.
        case Left(errorMessage) => {
          println(colour("Failed: ", Console.RED))
          println(s"\tStatus code: ${colour(res.code.toString, Console.YELLOW)}")
          println(s"\tResponse: ${colour(errorMessage.toString, Console.YELLOW)}")
          None
        }
        
        // Response was successful response.
        case Right(remoteViewResp) => {
          print(s"${colour("Success: ", Console.GREEN)}")
          
          if (isDebuggable) {
            println("Posted debugging stage of Debug Tree. Ready to post next stage")
          } else {
            println(s"${remoteViewResp.message}")
          }

          sessionId = remoteViewResp.sessionId
          Some(remoteViewResp)
        }
        
      }
      
    }
  }
}

/** Provides an API for creating new `RemoteView` instances */
object RemoteView {
  private val defaultPort: Int = 80
  private val defaultAddress: String = "127.0.0.1"

  private val dillPort: Int = 17484

  private final val MaxUserPort: Integer = 0xFFFF
  private final val MinimalIpLength: Int = "0.0.0.0".length
  private final val MaximalIpLength: Int = "255.255.255.255".length

  /** Creates a new RemoteView instance, which will send its HTTP requests to the specified port and address
   *
   * @param userPort The port to use
   * @param userAddress The address to use
   * @throws IllegalArgumentException if the provided port or address is invalid
   * @return A new instance of RemoteView
   */
  def apply(userPort: Int = defaultPort, userAddress: String = defaultAddress): RemoteView = new RemoteView {
    require(userPort <= MaxUserPort, s"Remote View port invalid : $port > $MaxUserPort")
    require(checkIp(userAddress), s"Remote View address invalid : $userAddress")

    override protected val port: Int = userPort
    override protected val address: String = userAddress
  }

  /** Connect to the DILL app (https://github.com/j-mie6/parsley-debug-app) running locally
    *
    * @return A new instance of `RemoteView`
    */
  def dill: RemoteView = RemoteView.dill(defaultAddress)

  /** Connect to the DILL app (https://github.com/j-mie6/parsley-debug-app) hosted externally
    *
    * @param userAddress The specific address hosting DILL
    * @throws IllegalArgumentException if the provided address is invalid
    * @return A new instance of `RemoteView`
    */
  def dill(userAddress: String): RemoteView = RemoteView(dillPort, userAddress)


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
    })
    
    addrLenValid && addrDotValid && addrNumValid 
  }

  /**
  * Default number of breakpoints skipped.
  * 
  * This value will be returned if
  * - RemoteView cannot connect to the specified server.
  * - Server does not actually return a value.
  * 
  * -1 represents that the parser should stop immediately. This is so 
  * that if the user is debugging infinite recursion, the lack of a valid
  * server will not cause the user's machine to burst into flames.
  */
  private [debug] final val DefaultBreakpointSkip = -1
}
