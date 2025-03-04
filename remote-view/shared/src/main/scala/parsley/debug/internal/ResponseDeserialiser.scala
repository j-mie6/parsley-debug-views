/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

import upickle.default.{ReadWriter => RW, *}
import parsley.debug.RemoteView

/**
  * Represents a generic response from the remote view.
  * 
  * By adding in the optional fields, that allows adding of arbitrary data, without
  * breaking back compatibility.
  *
  * @param message String response message from the remote view.
  * @param skipBreakpoint How many breakpoints to skip after this breakpoint (not required).
  */
private [debug] case class RemoteViewResponse(message: String, skipBreakpoint: Int = -1, newState: Option[Seq[RemoteView.State]] = None)

private [debug] object RemoteViewResponse {
  implicit val rw: RW[RemoteViewResponse] = macroRW
}
