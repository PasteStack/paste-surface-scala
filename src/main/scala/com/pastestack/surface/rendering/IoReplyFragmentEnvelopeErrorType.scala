package com.pastestack.surface.rendering

enum IoReplyFragmentEnvelopeErrorType(val code: Int, val wireName: String) {
  case ParamError extends IoReplyFragmentEnvelopeErrorType(400, "param_error")
  case AuthorizationError extends IoReplyFragmentEnvelopeErrorType(403, "authorization_error")
}
