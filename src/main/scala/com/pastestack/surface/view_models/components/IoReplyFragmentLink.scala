package com.pastestack.surface.view_models.components

final case class IoReplyFragmentLink(
  href: String,
  target: String,
  cssClass: String = ""
) {
  require(Option(href).exists(_.trim.nonEmpty), "IoReplyFragmentLink href must be nonblank")
  require(IoReplyFragmentTarget.isValidName(target), "IoReplyFragmentLink target must be a valid target name")
}
