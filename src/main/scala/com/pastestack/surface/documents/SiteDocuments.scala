// Copyright (c) 2017–present Tomshley LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
 *  a crawler's request. `canonicalOrigin` must be a clean `http`/`https`
 *  origin — scheme, host, and an optional port, with no userinfo, path,
 *  query, or fragment: the documents concatenate paths onto it, so an
 *  origin carrying components would render URLs nobody declared. A single
 *  trailing slash is normalized away. Every path is RFC 3986 path syntax:
 *  absolute, composed of unreserved/sub-delim/`:`/`@` characters plus
 *  well-formed percent-encodings — a malformed escape or an illegal raw
 *  character would render a URL that is broken no matter how the XML is
 *  escaped. The robots overlap check compares the paths a crawler sees:
 *  encodings of unreserved characters decode before the literal-prefix
 *  test (RFC 9309's normalization), and so do the operator escapes — a
 *  rule of `/path/foo-%24` matches the URI `/path/foo-$`, so declared
 *  operator literals and their encoded names compare in one form. Raw
 *  `*`/`$` are refused in `uncrawlablePaths` — they are matching
 *  operators in a Disallow rule, not literals, so a declared `*` would
 *  silently cover more than the invariant checked. The overlap check
 *  itself is a prefix test, not a segment match — `Disallow: /contact`
 *  also covers `/contactform`.
 */
final case class SiteDocuments(
    canonicalOrigin: String,
    indexablePaths: Seq[String],
    uncrawlablePaths: Seq[String] = Seq.empty
) {

  private val normalizedOrigin: String = canonicalOrigin.stripSuffix("/")
  require(SiteDocuments.isCleanOrigin(normalizedOrigin),
    "canonicalOrigin must be an absolute http(s) origin — scheme, host, optional port; no userinfo, path, query, or fragment")
  indexablePaths.foreach(path =>
    require(SiteDocuments.isUsablePath(path), s"indexable path must be a clean absolute path: $path"))
  uncrawlablePaths.foreach(path =>
    require(SiteDocuments.isUsablePath(path), s"uncrawlable path must be a clean absolute path: $path"))
  uncrawlablePaths.foreach(path =>
    require(!path.exists(SiteDocuments.robotsOperators.contains),
      s"uncrawlable path must be a literal prefix — robots matching operators are not accepted: $path"))

  private val renderedIndexable: Seq[String] = indexablePaths.map(SiteDocuments.canonicalForm)
  private val renderedUncrawlable: Seq[String] = uncrawlablePaths.map(SiteDocuments.canonicalForm)
  (renderedIndexable.iterator ++ renderedUncrawlable.iterator).foreach { path =>
    require(!path.split('/').exists(segment => segment == "." || segment == ".."),
      s"site document paths must not contain literal or encoded dot segments: $path")
  }
  private val matchingIndexable: Seq[String] = renderedIndexable.map(SiteDocuments.matchingForm)
  private val matchingUncrawlable: Seq[String] = renderedUncrawlable.map(SiteDocuments.matchingForm)

  indexablePaths.lazyZip(matchingIndexable).foreach { (declared, matching) =>
    require(!matchingUncrawlable.exists(matching.startsWith),
      s"indexable path sits under an uncrawlable prefix: $declared")
  }

  /** A wildcard user-agent group holding the declared disallow prefixes
   *  in crawler-normalized form — paths not matched are allowed by
   *  default, so no blanket `Allow` rule is emitted (an `Allow: /` would
   *  tie-break over `Disallow: /` and silently unexclude the root) — and
   *  a `Sitemap:` line derived from the same origin the sitemap renders:
   *  the two documents can never point at different hosts. */
  val robotsTxt: String =
    (Seq("User-agent: *") ++
      renderedUncrawlable.map(path => s"Disallow: $path") :+
      "" :+
      s"Sitemap: $normalizedOrigin/sitemap.xml").mkString("\n") + "\n"

  /** One `<url>` per indexable path in declared order, each rendered in
   *  its canonical encoding. No `lastmod` is emitted — a field without
   *  an honest content date teaches crawlers to ignore it. */
  val sitemapXml: String = {
    val entries = renderedIndexable.map(path =>
      s"  <url><loc>${SiteDocuments.xmlEscape(normalizedOrigin + path)}</loc></url>")
    (Seq("""<?xml version="1.0" encoding="UTF-8"?>""",
         """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""") ++
      entries :+ "</urlset>").mkString("\n") + "\n"
  }
}

object SiteDocuments {

  private val unreservedChars: Set[Char] =
    (('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ "-._~").toSet
  private val subDelims: Set[Char] = "!$&'()*+,;=".toSet

  /** Characters RFC 3986 permits raw in a path: unreserved, sub-delims,
   *  `:` and `@`, and the `/` separator. Everything else must arrive
   *  percent-encoded — and the encoding itself must be well-formed. */
  private val rawPathChars: Set[Char] = unreservedChars ++ subDelims ++ Set(':', '@', '/')

  /** `*` (any-length match) and `$` (end-of-path match) are matching
   *  operators in a Disallow rule — a declared prefix carrying one is a
   *  different promise than the literal overlap check verifies. */
  private val robotsOperators: Set[Char] = Set('*', '$')

  private val escapePattern: scala.util.matching.Regex = "%([0-9a-fA-F]{2})".r

  /** A clean origin is what `<loc>`/Sitemap concatenation can trust:
   *  scheme + host (+ port) and nothing else. `URI.getHost` is null for
   *  authorities that do not parse to a host — bare ports, underscores —
   *  so those are refused by the host check alone. */
  private def isCleanOrigin(value: String): Boolean =
    try {
      val uri = new URI(value)
      (uri.getScheme == "http" || uri.getScheme == "https") &&
        uri.getHost != null && uri.getHost.nonEmpty &&
        uri.getHost.forall(_ <= 0x7f) &&
        uri.getRawUserInfo == null &&
        (uri.getRawPath == null || uri.getRawPath.isEmpty) &&
        uri.getRawQuery == null &&
        uri.getRawFragment == null &&
        uri.getPort != 0 && uri.getPort <= 65535
    } catch {
      case _: URISyntaxException => false
    }

  private def isHexDigit(char: Char): Boolean =
    (char >= '0' && char <= '9') || (char >= 'a' && char <= 'f') || (char >= 'A' && char <= 'F')

  /** Paths land inside generated URLs and crawlers' fetch queues, so
   *  they must be real URI paths: absolute, every raw character an RFC
   *  3986 path character, and every `%` the lead of a complete hex
   *  escape. A path that passes here renders a valid URL — escaping the
   *  XML around it is presentation, not rescue. */
  private def isUsablePath(path: String): Boolean = {
    def completeEscape(index: Int): Boolean =
      index + 2 < path.length &&
        isHexDigit(path.charAt(index + 1)) && isHexDigit(path.charAt(index + 2))
    path.startsWith("/") &&
      path.forall(char => char == '%' || rawPathChars.contains(char)) &&
      path.indices.filter(index => path.charAt(index) == '%').forall(completeEscape)
  }

  /** The canonical encoding: escapes of unreserved characters decode to
   *  the literal — `/%63ontact` IS `/contact` — while every other escape
   *  is canonicalized to uppercase hex so `%2f` and `%2F` name one
   *  octet. This is the form both rendered documents emit, and the base
   *  the robots-matching form is derived from. */
  private def canonicalForm(path: String): String =
    escapePattern.replaceAllIn(path, matched => {
      val decoded = Integer.parseInt(matched.group(1), 16).toChar
      if (unreservedChars.contains(decoded)) decoded.toString
      else "%" + matched.group(1).toUpperCase
    })

  /** The path the crawler's matcher effectively sees — the canonical
   *  form plus the robots operator escapes decoded to their literals:
   *  RFC 9309 matches `%2A` and `%24` in a rule against literal `*`/`$`
   *  characters in a URI, so the declared literal and its encoded name
   *  must compare equal here. Other escapes stay encoded — an encoded
   *  separator like `%2F` is not the path separator it spells. Applied
   *  only to overlap checking, never to emitted text: an emitted
   *  `Disallow: /foo-$` would read `$` as an end anchor. */
  private def matchingForm(path: String): String =
    path.replace("%2A", "*").replace("%24", "$")

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
