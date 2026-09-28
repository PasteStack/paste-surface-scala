package com.pastestack.surface.documents

import java.net.{URI, URISyntaxException}

/** The origin-level documents a paste site answers — `/robots.txt` and
 *  `/sitemap.xml` — rendered from declared facts rather than shipped as
 *  static files. A checked-in sitemap drifts from the routes, and a
 *  checked-in `robots.txt` hardcodes a host that a preview or local
 *  response would then serve as fact.
 *
 *  The site supplies three facts: the canonical origin the documents
 *  advertise (where the site should be indexed from, independent of which
 *  host answered), the public page inventory, and the path prefixes
 *  cooperative crawlers must not fetch — token-bearing or otherwise
 *  non-content namespaces. Everything rendered is deterministic.
 *
 *  Invariants hold at construction: an invalid model fails wiring, never
 *  a crawler's request. `canonicalOrigin` must be an absolute `http` or
 *  `https` URI with a non-empty host, and is rendered without a trailing
 *  slash. Every path begins with `/`, and no indexable path may sit under
 *  an uncrawlable prefix — RFC 9309 matching is literal path-prefix, so
 *  `Disallow: /contact` also covers `/contactform`; the overlap check is a
 *  `startsWith`, not a segment match.
 */
final case class SiteDocuments(
    canonicalOrigin: String,
    indexablePaths: Seq[String],
    uncrawlablePaths: Seq[String] = Seq.empty
) {

  private val normalizedOrigin: String = canonicalOrigin.stripSuffix("/")
  require(SiteDocuments.isAbsoluteHttpUri(normalizedOrigin),
    "canonicalOrigin must be an absolute http(s) URI with a host")
  indexablePaths.foreach(path =>
    require(path.startsWith("/"), s"indexable path must start with '/': $path"))
  uncrawlablePaths.foreach(path =>
    require(path.startsWith("/"), s"uncrawlable path must start with '/': $path"))
  indexablePaths.foreach(path =>
    require(!uncrawlablePaths.exists(path.startsWith),
      s"indexable path sits under an uncrawlable prefix: $path"))

  /** An allow-all user-agent group, the declared disallow prefixes, and a
   *  `Sitemap:` line derived from the same origin the sitemap renders —
   *  the two documents can never point at different hosts. */
  val robotsTxt: String =
    (Seq("User-agent: *", "Allow: /") ++
      uncrawlablePaths.map(path => s"Disallow: $path") :+
      "" :+
      s"Sitemap: $normalizedOrigin/sitemap.xml").mkString("\n") + "\n"

  /** One `<url>` per indexable path in declared order. No `lastmod` is
   *  emitted — a field without an honest content date teaches crawlers to
   *  ignore it. */
  val sitemapXml: String = {
    val entries = indexablePaths.map(path =>
      s"  <url><loc>${SiteDocuments.xmlEscape(normalizedOrigin + path)}</loc></url>")
    (Seq("""<?xml version="1.0" encoding="UTF-8"?>""",
         """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""") ++
      entries :+ "</urlset>").mkString("\n") + "\n"
  }
}

object SiteDocuments {

  private def isAbsoluteHttpUri(value: String): Boolean =
    try {
      val uri = new URI(value)
      (uri.getScheme == "http" || uri.getScheme == "https") &&
        uri.getHost != null && uri.getHost.nonEmpty
    } catch {
      case _: URISyntaxException => false
    }

  /** `<loc>` content is element text — a configured origin carrying markup
   *  characters must not be able to break the document. */
  private def xmlEscape(text: String): String =
    text.flatMap {
      case '&' => "&amp;"
      case '<' => "&lt;"
      case '>' => "&gt;"
      case '"' => "&quot;"
      case '\'' => "&apos;"
      case char => char.toString
    }
}
