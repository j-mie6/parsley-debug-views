package parsley.debug.internal

import sttp.client3._
import sttp.client3.upicklejson._
import upickle.default._

val backend: SttpBackend[Identity, Any] = HttpClientSyncBackend()

implicit val requestPayloadRW: ReadWriter[RequestPayload] = macroRW[RequestPayload]
implicit val responsePayloadRW: ReadWriter[ResponsePayload] = macroRW[ResponsePayload]

val requestPayload = RequestPayload("some data")

val response: Identity[Response[Either[ResponseException[String, Exception], ResponsePayload]]] =
basicRequest
  .post(uri"...")
  .body(requestPayload)
  .response(asJson[ResponsePayload])
  .send(backend)

private sealed case class RemoteViewResponse(message: String, options: RemoteViewOptions)