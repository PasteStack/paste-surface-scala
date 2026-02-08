package com.pastestack.surface.view_models.page

case class PageMeta(
  siteName: String,
  title: String,
  description: String,
  keywords: String = "",
  url: String = "",
  themeColor: String = "",
  fbSiteType: Option[String] = Option.empty,
  domainName: String = ""
)
