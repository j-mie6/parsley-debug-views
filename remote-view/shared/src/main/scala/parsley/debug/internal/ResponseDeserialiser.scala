/*
* Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
*
* SPDX-License-Identifier: BSD-3-Clause
*/
package parsley.debug.internal

import upickle.default as up

import parsley.debug.RemoteView
import parsley.debug.RefCodec.CodedRef

/**
* Represents a generic response from the remote view.
* 
* By adding in the optional fields, that allows adding of arbitrary data, without
* breaking back compatibility.
*
* @param message String response message from the remote view.
* @param skipBreakpoint How many breakpoints to skip after this breakpoint (not required).
*/
private [debug] case class RemoteViewResponse(message: String, skipBreakpoint: Int = -1, newRefs: Seq[CodedRef] = Nil)

private [debug] object RemoteViewResponse {
  implicit val rw: up.ReadWriter[RemoteViewResponse] = up.macroRW
  
  implicit class RemoteViewResponseExtensions(resp: Option[RemoteViewResponse]) {
    /** Get number of breakpoints to skip from optional response */
    def getSkipsOrDefault: Int = resp.map(_.skipBreakpoint).getOrElse(RemoteView.DefaultBreakpointSkip)
    
    /** Get updated new refs from optional response */
    def getNewRefsOrDefault: Seq[CodedRef] = resp.map(_.newRefs).getOrElse(Nil)
  }
}
