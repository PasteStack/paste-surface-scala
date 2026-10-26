package com.pastestack.surface

import com.pastestack.surface.view_models.page.PageMeta
import com.pastestack.surface.view_model_factories.PageMetaFactory
import play.twirl.api.Html
import scala.util.Try

object PageMetaRenderingChecks {
  def main(args: Array[String]): Unit = {
    val empty = Html("")
    def verify(meta: PageMeta, expected: String): Unit = {
      val pages = Seq(
        paste.surface.layouts.html.webbase(meta)(empty)(empty)(empty)(empty)(empty).body,
        paste.surface.layouts.html.jambase(meta)(empty)(empty)(empty)(empty)(empty).body)
      assert(meta.canonicalUrl == expected)
      pages.foreach { html =>
        if (expected.isEmpty) {
          assert(!html.contains("rel=\"canonical\""))
          assert(!html.contains("property=\"og:url\""))
        } else {
          assert(html.contains(s"""rel="canonical" href="$expected""""))
          assert(html.contains(s"""property="og:url" content="$expected""""))
        }
      }
    }
    for ((authored, expected) <- Seq(
      "" -> "", "example.com/article" -> "https://example.com/article",
      "//example.com/a" -> "https://example.com/a",
      "https://example.com/article" -> "https://example.com/article",
      "http://localhost:8080/a" -> "http://localhost:8080/a")) {
      val meta = PageMeta("Example", "Title", "Description", url = authored)
      assert(meta.url == authored)
      verify(meta, expected)
    }
    verify(PageMetaFactory.forHomePage(PageMetaFactory.SiteConfig("Example", "example.com")), "https://example.com")
    for (url <- Seq(null, "https:/example.com", "http:example.com", "ftp://example.com", "https://user@example.com", "/relative", "https://example.com:65536")) {
      assert(Try(PageMeta("Example", "Title", "Description", url = url)).isFailure)
      assert(Try(PageMeta("Example", "Title", "Description").copy(url = url)).isFailure)
    }
  }
}
