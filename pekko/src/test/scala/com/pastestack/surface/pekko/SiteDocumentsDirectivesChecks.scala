package com.pastestack.surface.pekko

import com.pastestack.surface.documents.SiteDocuments
import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SiteDocumentsDirectivesChecks extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  private val documents = SiteDocuments(
    canonicalOrigin = "https://www.example.com",
    indexablePaths = Seq("/", "/privacy"),
    uncrawlablePaths = Seq("/contact"))

  "siteDocumentRoutes" should "serve robots.txt as text/plain" in {
    Get("/robots.txt") ~> SiteDocumentsDirectives.siteDocumentRoutes(documents) ~> check {
      status shouldBe StatusCodes.OK
      response.entity.contentType shouldBe ContentTypes.`text/plain(UTF-8)`
      responseAs[String] should include("Disallow: /contact")
      responseAs[String] should include("Sitemap: https://www.example.com/sitemap.xml")
    }
  }

  it should "serve sitemap.xml as text/xml enumerating the indexable paths" in {
    Get("/sitemap.xml") ~> SiteDocumentsDirectives.siteDocumentRoutes(documents) ~> check {
      status shouldBe StatusCodes.OK
      response.entity.contentType shouldBe ContentTypes.`text/xml(UTF-8)`
      responseAs[String] should include("<loc>https://www.example.com/</loc>")
      responseAs[String] should include("<loc>https://www.example.com/privacy</loc>")
      responseAs[String] should not include "<loc>https://www.example.com/contact</loc>"
    }
  }

  it should "let a site bind each document on its own" in {
    Get("/robots.txt") ~> SiteDocumentsDirectives.robotsRoute(documents) ~> check {
      status shouldBe StatusCodes.OK
    }
    Get("/sitemap.xml") ~> SiteDocumentsDirectives.robotsRoute(documents) ~> check {
      handled shouldBe false
    }
    Get("/sitemap.xml") ~> SiteDocumentsDirectives.sitemapRoute(documents) ~> check {
      status shouldBe StatusCodes.OK
    }
    Get("/robots.txt") ~> SiteDocumentsDirectives.sitemapRoute(documents) ~> check {
      handled shouldBe false
    }
  }

  it should "reject an invalid model at construction, not at request time" in {
    an[IllegalArgumentException] should be thrownBy
      SiteDocuments("example.com", Seq("/"), Seq.empty)
  }
}
