package com.pastestack.surface.rendering

final case class Rendered(contentType: String, body: String)

object Rendered {
  val HtmlContentType: String = "text/html; charset=utf-8"
  val JsonContentType: String = "application/json"
}
