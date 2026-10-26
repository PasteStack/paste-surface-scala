package com.pastestack.surface

import com.pastestack.surface.documents.SiteDocuments

import scala.util.{Failure, Try}

object SiteDocumentsChecks {
  private def rejected(f: => Any): Boolean =
    Try(f) match {
      case Failure(_: IllegalArgumentException) => true
      case _ => false
    }

  def main(args: Array[String]): Unit = {
    for (path <- Seq("/public/../private/record", "/public/%2e%2E/private", "/./a", "/a/.", "/a/%2e", "/a/..", "/a/.%2E")) {
      assert(rejected(SiteDocuments("https://example.com", Seq(path), Seq("/private"))))
      assert(rejected(SiteDocuments("https://example.com", Seq.empty, Seq(path))))
      assert(rejected(SiteDocuments("https://example.com", Seq.empty).copy(indexablePaths = Seq(path))))
    }
    assert(!rejected(SiteDocuments("https://example.com", Seq("/a/.../b"))))
    val documents = SiteDocuments(
      canonicalOrigin = "https://www.example.com",
      indexablePaths = Seq("/", "/privacy", "/terms"),
      uncrawlablePaths = Seq("/contact"))

    // robots.txt renders the wildcard group, the declared disallows, and
    // the Sitemap: line derived from the same origin — unmatched paths
    // are allowed by default, so no blanket Allow rule exists to
    // tie-break over a root Disallow.
    assert(documents.robotsTxt ==
      "User-agent: *\nDisallow: /contact\n\nSitemap: https://www.example.com/sitemap.xml\n")

    // sitemap.xml enumerates exactly the indexable paths, in order.
    assert(documents.sitemapXml ==
      """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" +
      """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""" + "\n" +
      "  <url><loc>https://www.example.com/</loc></url>\n" +
      "  <url><loc>https://www.example.com/privacy</loc></url>\n" +
      "  <url><loc>https://www.example.com/terms</loc></url>\n" +
      "</urlset>\n")

    // With no disallows the group has no rules — the default is allow.
    assert(SiteDocuments("https://example.com", Seq("/"), Seq.empty).robotsTxt ==
      "User-agent: *\n\nSitemap: https://example.com/sitemap.xml\n")

    // A root disallow must stand alone: the old unconditional Allow: /
    // would have tied on length and won, defeating the exclusion.
    assert(SiteDocuments("https://example.com", Seq.empty, Seq("/")).robotsTxt ==
      "User-agent: *\nDisallow: /\n\nSitemap: https://example.com/sitemap.xml\n")

    // A trailing slash on the configured origin is normalized, not doubled.
    assert(SiteDocuments("https://example.com/", Seq("/"), Seq.empty).robotsTxt
      .contains("Sitemap: https://example.com/sitemap.xml"))

    // The origin must be absolute http(s) with a host — not relative, not
    // another scheme, not empty. An explicit port stays an origin.
    assert(!rejected(SiteDocuments("http://localhost:8080", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("example.com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("ftp://example.com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://", Seq("/"), Seq.empty)))

    // But no components beyond the origin: the documents concatenate
    // paths onto it, so userinfo, a path, a query, or a fragment would
    // emit URLs nobody declared — and a nonsense port renders dead links.
    assert(rejected(SiteDocuments("https://example.com/?a=1&b=2", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com/base/path", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://user:pw@example.com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com#frag", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com:99999", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com:0", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example .com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://café.example.com", Seq("/"), Seq.empty)),
      "a non-ASCII host must be configured as punycode")

    // Every declared path must be absolute.
    assert(rejected(SiteDocuments("https://example.com", Seq("privacy"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/"), Seq("contact"))))

    // Paths land inside generated URLs — they must be real RFC 3986
    // paths: valid escapes pass; malformed escapes, illegal raw
    // characters, control characters, space, and the `?`/`#` that would
    // inject components are refused. Escaping the XML cannot rescue a
    // URL that is already broken.
    assert(!rejected(SiteDocuments("https://example.com", Seq("/a%20b"), Seq.empty)))
    assert(!rejected(SiteDocuments("https://example.com", Seq("/a%2Fb"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/q?x=1"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/q#frag"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/bad path"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/bad%GG"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/bad%2"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/bad%"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/a\\b"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/a|b"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/<tag>"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/a\"b"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/a`b"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/bad\npath"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/caf\u00e9"), Seq.empty)),
      "a non-ASCII path emits raw UTF-8 into <loc> — it must be declared %-encoded")
    assert(rejected(SiteDocuments("https://example.com", Seq("/"), Seq("/x?y"))))
    assert(rejected(SiteDocuments("https://example.com", Seq("/"), Seq("/bad%GG"))))

    // Both rendered documents carry the canonical encoding — an
    // unreserved-encoded path is rendered literal, and a reserved
    // escape renders uppercase-hex.
    assert(SiteDocuments("https://example.com", Seq("/a%63b"), Seq.empty).sitemapXml
      .contains("<loc>https://example.com/acb</loc>"))
    assert(SiteDocuments("https://example.com", Seq("/x"), Seq("/a%63b")).robotsTxt
      .contains("Disallow: /acb"))
    assert(SiteDocuments("https://example.com", Seq("/a%2fb"), Seq.empty).sitemapXml
      .contains("<loc>https://example.com/a%2Fb</loc>"))

    // No indexable path may sit under an uncrawlable prefix — literal
    // prefix matching, so "/contact" also excludes "/contactform".
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact/t1"), Seq("/contact"))))
    assert(rejected(SiteDocuments("https://example.com", Seq("/contactform"), Seq("/contact"))))
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact"), Seq("/contact"))))

    // The overlap check compares the path a crawler sees: an encoding
    // cannot smuggle an indexable path under a disallow — and a robots
    // matching operator is never accepted as a declared prefix.
    assert(rejected(SiteDocuments("https://example.com", Seq("/%63ontact"), Seq("/contact"))),
      "an unreserved-encoded indexable path still overlaps its decoded form")
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact"), Seq("/%63ontact"))),
      "an unreserved-encoded disallow still covers its decoded path")
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact/public"), Seq("/contact*"))),
      "* in a Disallow rule is a matcher, not a literal — it would cover more than checked")
    assert(rejected(SiteDocuments("https://example.com", Seq("/x"), Seq("/contact$"))),
      "$ in a Disallow rule is an end anchor, not a literal")

    // RFC 9309 §2.2.3 Figure 6: the encoded operator names in a rule
    // match the literal characters in a URI, so overlap must compare
    // them in one form — `/path/foo-%24` DOES cover `/path/foo-$`.
    assert(rejected(SiteDocuments("https://example.com", Seq("/path/foo-$"), Seq("/path/foo-%24"))),
      "an encoded end-anchor literal still covers the raw path")
    assert(rejected(SiteDocuments("https://example.com", Seq("/path/foo-*"), Seq("/path/foo-%2A"))),
      "an encoded wildcard literal still covers the raw path")
    assert(rejected(SiteDocuments("https://example.com", Seq("/path/foo-%24"), Seq("/path/foo-%24"))),
      "identical encoded literals still overlap")
    assert(!rejected(SiteDocuments("https://example.com", Seq("/a%2Fb"), Seq("/a/b"))),
      "an encoded separator is not the separator it spells")
  }
}
