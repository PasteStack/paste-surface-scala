package com.pastestack.surface.view_models.components

case class Logo(
  href: String = "/",
  ariaLabel: String,
  title: String,
  width: Int,
  height: Int,
  viewBox: Option[String] = None,
  cssClass: String = ""
)
