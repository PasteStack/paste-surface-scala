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
) {
  // Preserve the authored member; both layouts use one resolved URL.
  val canonicalUrl: String = PageMeta.canonicalUrl(url)
}

object PageMeta {
  private val Scheme = "^[A-Za-z][A-Za-z0-9+.-]*://".r

  private def canonicalUrl(value: String): String = {
    require(value != null, "PageMeta url must not be null")
    if (value.isEmpty) ""
    else {
      val absolute =
        if (value.startsWith("//")) "https:" + value
        else if (value.regionMatches(true, 0, "http:", 0, 5) ||
          value.regionMatches(true, 0, "https:", 0, 6) || Scheme.findPrefixOf(value).isDefined) value
        else "https://" + value
      val uri = java.net.URI.create(absolute)
      require(
        (uri.getScheme.equalsIgnoreCase("http") || uri.getScheme.equalsIgnoreCase("https")) &&
          uri.getHost != null && uri.getRawUserInfo == null && uri.getPort <= 65535,
        "PageMeta url must be an http(s) URL or a scheme-less host/path; declare international hosts as punycode"
      )
      absolute
    }
  }
}
