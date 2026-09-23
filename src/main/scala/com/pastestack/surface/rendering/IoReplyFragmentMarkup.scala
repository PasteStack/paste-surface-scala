package com.pastestack.surface.rendering

import play.twirl.api.Html

final case class IoReplyFragmentMarkup(
  markup: Html,
  js: Seq[String] = Seq.empty,
  css: Seq[String] = Seq.empty
) {
  require(Option(markup).exists(_.body != null), "IO reply fragment markup must be set")
  require(Option(js).exists(_.forall(_ != null)), "IO reply fragment markup js must be set without null entries")
  require(Option(css).exists(_.forall(_ != null)), "IO reply fragment markup css must be set without null entries")
}
