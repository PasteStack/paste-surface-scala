package com.pastestack.surface.view_models.components

final case class IoReplyFragmentTarget(
  name: String,
  cssClass: String = "",
  format: IoReplyFragmentFormat = IoReplyFragmentFormat.Html
) {
  require(format != null, "IoReplyFragmentTarget format must be set")
  require(IoReplyFragmentTarget.isValidName(name),
    s"IoReplyFragmentTarget name must be nonblank without whitespace, got: $name")
}

object IoReplyFragmentTarget {
  def isValidName(name: String): Boolean =
    Option(name).exists(n => n.nonEmpty && !n.exists(_.isWhitespace))
}
