package com.pastestack.surface.rendering

final case class IoReplyFragmentEnvelopeError(
  errorType: IoReplyFragmentEnvelopeErrorType,
  detail: String = ""
) {
  require(errorType != null, "IO reply fragment envelope error type must be set")
  require(detail != null, "IO reply fragment envelope error detail must be set")
}
