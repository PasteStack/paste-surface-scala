package com.pastestack.surface.rendering

import play.twirl.api.Html

final case class Presentation[A](
  page: A => Html,
  ioReplyFragment: A => Html,
  json: Option[ViewModelJson[A]] = None
) {
  def render(model: A, rendering: Rendering): Option[Rendered] = rendering match {
    case Rendering.Page =>
      Some(Rendered(Rendered.HtmlContentType, page(model).body))
    case Rendering.IoReplyFragment =>
      Some(Rendered(Rendered.HtmlContentType, ioReplyFragment(model).body))
    case Rendering.Json =>
      json.map(encoder => Rendered(Rendered.JsonContentType, encoder.encode(model)))
  }
}
