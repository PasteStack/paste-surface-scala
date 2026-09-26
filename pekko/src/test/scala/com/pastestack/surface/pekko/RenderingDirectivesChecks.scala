package com.pastestack.surface.pekko

import com.pastestack.surface.rendering.{Presentation, Rendering}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCode, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.complete
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.twirl.api.Html

class RenderingDirectivesChecks extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  private def varyValues: Seq[String] =
    response.headers.filter(_.is("vary")).map(_.value())

  private val presentation = Presentation[String](
    page = model => Html(s"<html><body>page:$model</body></html>"),
    ioReplyFragment = model => Html(s"<div>fragment:$model</div>"),
    json = Some((model: String) => s"""{"value":"$model"}""")
  )

  private val presentationNoJson = Presentation[String](
    page = model => Html(s"<html><body>page:$model</body></html>"),
    ioReplyFragment = model => Html(s"<div>fragment:$model</div>")
  )

  private def routeFor(p: Presentation[String], status: StatusCode = StatusCodes.OK): Route =
    RenderingDirectives.rendering { r =>
      RenderingDirectives.completeRendered(status, p, "model", r)
    }

  "rendering" should "serve the io-reply fragment for an X-Requested-With request" in {
    Get().withHeaders(RawHeader("X-Requested-With", "XMLHttpRequest")) ~> routeFor(presentation) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "<div>fragment:model</div>"
      varyValues shouldBe Seq(Rendering.VaryHeader)
    }
  }

  it should "serve the full page for a plain request" in {
    Get() ~> routeFor(presentation) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "<html><body>page:model</body></html>"
      varyValues shouldBe Seq(Rendering.VaryHeader)
    }
  }

  it should "serve the JSON encoding for an Accept: application/json request" in {
    Get().withHeaders(RawHeader("Accept", "application/json")) ~> routeFor(presentation) ~> check {
      status shouldBe StatusCodes.OK
      response.entity.contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe """{"value":"model"}"""
      varyValues shouldBe Seq(Rendering.VaryHeader)
    }
  }

  it should "respond 406 for JSON when the presentation has no encoder" in {
    Get().withHeaders(RawHeader("Accept", "application/json")) ~> routeFor(presentationNoJson) ~> check {
      status shouldBe StatusCodes.NotAcceptable
      varyValues shouldBe Seq(Rendering.VaryHeader)
    }
  }

  it should "select the rendering from the request headers" in {
    val selected = RenderingDirectives.rendering { r => complete(s"$r") }
    Get().withHeaders(RawHeader("X-Requested-With", "XMLHttpRequest")) ~> selected ~> check {
      responseAs[String] shouldBe "IoReplyFragment"
    }
    Get().withHeaders(RawHeader("Accept", "application/json")) ~> selected ~> check {
      responseAs[String] shouldBe "Json"
    }
    Get() ~> selected ~> check {
      responseAs[String] shouldBe "Page"
    }
  }

  it should "render off the request dispatcher" in {
    @volatile var renderingThread = ""
    val recording = Presentation[String](
      page = model => {
        renderingThread = Thread.currentThread().getName
        Html(s"<html><body>page:$model</body></html>")
      },
      ioReplyFragment = model => Html(s"<div>fragment:$model</div>")
    )
    Get() ~> routeFor(recording) ~> check {
      status shouldBe StatusCodes.OK
      renderingThread should include("default-blocking-io-dispatcher")
    }
  }

  it should "preserve the caller's status on the rendered response" in {
    Get().withHeaders(RawHeader("X-Requested-With", "XMLHttpRequest")) ~>
      routeFor(presentationNoJson, StatusCodes.BadRequest) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe "<div>fragment:model</div>"
        varyValues shouldBe Seq(Rendering.VaryHeader)
      }
  }
}
