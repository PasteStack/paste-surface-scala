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
    val documents = SiteDocuments(
      canonicalOrigin = "https://www.example.com",
      indexablePaths = Seq("/", "/privacy", "/terms"),
      uncrawlablePaths = Seq("/contact"))

    // robots.txt renders the allow-all group, the declared disallows, and
    // the Sitemap: line derived from the same origin.
    assert(documents.robotsTxt ==
      "User-agent: *\nAllow: /\nDisallow: /contact\n\nSitemap: https://www.example.com/sitemap.xml\n")

    // sitemap.xml enumerates exactly the indexable paths, in order.
    assert(documents.sitemapXml ==
      """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" +
      """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""" + "\n" +
      "  <url><loc>https://www.example.com/</loc></url>\n" +
      "  <url><loc>https://www.example.com/privacy</loc></url>\n" +
      "  <url><loc>https://www.example.com/terms</loc></url>\n" +
      "</urlset>\n")

    // With no disallows the file is pure allow-all.
    assert(SiteDocuments("https://example.com", Seq("/"), Seq.empty).robotsTxt ==
      "User-agent: *\nAllow: /\n\nSitemap: https://example.com/sitemap.xml\n")

    // A trailing slash on the configured origin is normalized, not doubled.
    assert(SiteDocuments("https://example.com/", Seq("/"), Seq.empty).robotsTxt
      .contains("Sitemap: https://example.com/sitemap.xml"))

    // The origin must be absolute http(s) with a host — not relative, not
    // another scheme, not empty.
    assert(rejected(SiteDocuments("example.com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("ftp://example.com", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("", Seq("/"), Seq.empty)))
    assert(rejected(SiteDocuments("https://", Seq("/"), Seq.empty)))

    // Every declared path must be absolute.
    assert(rejected(SiteDocuments("https://example.com", Seq("privacy"), Seq.empty)))
    assert(rejected(SiteDocuments("https://example.com", Seq("/"), Seq("contact"))))

    // No indexable path may sit under an uncrawlable prefix — literal
    // prefix matching, so "/contact" also excludes "/contactform".
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact/t1"), Seq("/contact"))))
    assert(rejected(SiteDocuments("https://example.com", Seq("/contactform"), Seq("/contact"))))
    assert(rejected(SiteDocuments("https://example.com", Seq("/contact"), Seq("/contact"))))

    // Markup characters in a configured origin cannot break <loc> — `&` is
    // the one escapable character a legal URI can carry.
    assert(SiteDocuments("https://example.com/?a=1&b=2", Seq("/p"), Seq.empty).sitemapXml
      .contains("https://example.com/?a=1&amp;b=2/p"))
  }
}
