package com.pastestack.surface.view_models.components

final case class LazyContent(
  url: String,
  id: Option[String] = None,
  cssClass: String = ""
) {
  require(Option(url).exists(_.trim.nonEmpty), "LazyContent URL must be nonblank")
  require(id != null, "LazyContent id must not be null")
  id.foreach(GeneratedId.requireHtmlId("LazyContent id", _))
}
