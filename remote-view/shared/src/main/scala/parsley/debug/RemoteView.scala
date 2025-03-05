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

import upickle.default.{ReadWriter => RW, *}

import sttp.client3.*
import sttp.client3.upicklejson.*

import parsley.debug.internal.DebugTreeSerialiser
import parsley.debug.internal.RemoteViewResponse
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
    
    // Printing helpers
    private [debug] final val TextToRed    = "\u001b[31m"
    private [debug] final val TextToGreen  = "\u001b[92m"
    private [debug] final val TextToOrange = "\u001b[93m"
    private [debug] final val TextToNormal = "\u001b[0m"
    
    // Request Timeouts
    private [debug] final val ConnectionTimeout = 30.second
    private [debug] final val ResponseTimeout   = 10.second
    private [debug] final val BreakpointTimeout = 30.minute
    
    // Endpoint for post request
    private [debug] final lazy val endPoint = uri"http://$address:$port/api/remote/tree"
    
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
    
    /**
     * Send the debug tree and input to the port and address specified in the 
     * object construction. 
     *
     * @param input The input source.
     * @param tree The debug tree.
     */
    override private [debug] def render(input: => String, tree: => DebugTree): Unit = {
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
     * @param timeout The maximal timeout of the connection.
     * @param isDebuggable If the instance is a debuggable instance.
     * 
     * @return The number of breakpoints to skip after this breakpoint exits.
     */
    override private [debug] def renderWait(input: => String, tree: => DebugTree): Int = {
        renderWithTimeout(input, tree, BreakpointTimeout, isDebuggable = true).getSkips
    }
    
    
    override private [debug] def renderManage(input: => String, tree: => DebugTree, refs: CodedRef*): (Int, Seq[CodedRef]) = {
        val resp: Option[RemoteViewResponse] = renderWithTimeout(input, tree, BreakpointTimeout, isDebuggable = true, refs.toSeq)
        (resp.getSkips, resp.getNewRefs)
    }
    
    private [debug] def renderWithTimeout(input: => String, tree: => DebugTree, timeout: FiniteDuration, isDebuggable: Boolean = false, refs: Seq[CodedRef] = Nil): Option[RemoteViewResponse] = {
        // JSON formatted payload for post request
        val payload: String = DebugTreeSerialiser.toJSON(input, tree, isDebuggable, refs)
        
        // Send POST
        println("Sending Debug Tree to Server...")
        if (isDebuggable) println("\tWaiting for debugging input...")
        if (refs.nonEmpty) println("\tManaging state...")
        
        // Implicit JSON deserialiser
        implicit val responsePayloadRW: RW[RemoteViewResponse] = macroRW[RemoteViewResponse]
        
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
                println(s"${TextToRed}Remote View request failed! ${TextToNormal}" +
                s"Please validate address (${TextToOrange}$address${TextToNormal}) and " +
                s"port number (${TextToOrange}$port${TextToNormal}) and " +
                s"make sure the Remote View app is running.")
                
                println(s"\t${TextToRed}Error: ${TextToNormal}${exception.toString}")
                None
            }
            
            // POST request was successful
            case Success(res) => res.body match {
                // Response was failed response.
                case Left(errorMessage) => {
                    println(s"${TextToRed}Failed: ${TextToNormal}Status code: ${TextToOrange}${res.code}${TextToNormal}, Response: $errorMessage")
                    None
                }
                
                // Response was successful response.
                case Right(remoteViewResp) => {
                    println(s"${TextToGreen}Success: ${TextToNormal}${remoteViewResp.message}")
                    Some(remoteViewResp)
                }
                
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
        })
        
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
