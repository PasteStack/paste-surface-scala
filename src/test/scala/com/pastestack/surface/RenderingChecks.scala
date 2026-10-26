package com.pastestack.surface

import com.pastestack.surface.presenters.PagePresenter
import com.pastestack.surface.rendering._
import play.twirl.api.Html

import scala.util.{Failure, Try}

object RenderingChecks {
  private def rejected(f: => Any): Boolean =
    Try(f) match {
      case Failure(_: IllegalArgumentException) => true
      case _ => false
    }

  def main(args: Array[String]): Unit = {
    val jsWhitespace = Seq(0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020,
      0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005,
      0x2006, 0x2007, 0x2008, 0x2009, 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF)
    jsWhitespace.foreach { code =>
      assert(rejected(IoReplyFragmentEnvelope.success(Seq.empty, Seq((code.toChar.toString + "message") -> "value"), 0)))
    }
    assert(IoReplyFragmentEnvelope.success(Seq.empty, Seq("café" -> "value"), 0).contains("café"))
    assert(Rendering.VaryHeader == "X-Requested-With, Accept")

    // Selection: Json only when the client asks for JSON and cannot take HTML;
    // else IoReplyFragment on X-Requested-With; else Page.
    assert(Rendering.select(None, None) == Rendering.Page)
    assert(Rendering.select(None, Some("text/html")) == Rendering.Page)
    assert(Rendering.select(None, Some("*/*")) == Rendering.Page)
    assert(Rendering.select(Some("XMLHttpRequest"), None) == Rendering.IoReplyFragment)
    assert(Rendering.select(Some("xmlhttprequest"), None) == Rendering.IoReplyFragment)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("*/*")) == Rendering.IoReplyFragment)
    assert(Rendering.select(Some("fetch"), None) == Rendering.Page)
    assert(Rendering.select(None, Some("application/json")) == Rendering.Json)
    assert(Rendering.select(None, Some("Application/JSON")) == Rendering.Json)
    assert(Rendering.select(None, Some("application/json;q=0.9")) == Rendering.Json)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("application/json")) == Rendering.Json)
    assert(Rendering.select(None, Some("text/html, application/json")) == Rendering.Page)
    assert(Rendering.select(None, Some("text/html;q=1.0, application/json;q=0.9")) == Rendering.Page)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("application/json, text/html")) == Rendering.IoReplyFragment)
    assert(Rendering.select(None, Some("application/problem+json")) == Rendering.Page)
    assert(Rendering.select(None, Some("application/json;q=0")) == Rendering.Page)
    assert(Rendering.select(None, Some("application/json; Q=0.000")) == Rendering.Page)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("application/json;q=0")) == Rendering.IoReplyFragment)
    assert(Rendering.select(None, Some("text/html;q=0, application/json")) == Rendering.Json)
    assert(Rendering.select(None, Some("application/json;q=abc")) == Rendering.Json)
    assert(Rendering.select(None, Some(";")) == Rendering.Page)
    assert(Rendering.select(None, Some(";;")) == Rendering.Page)
    assert(Rendering.select(None, Some("application/json, ;")) == Rendering.Json)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("text/html, ;")) == Rendering.IoReplyFragment)

    // Media ranges lowercase against a fixed locale: under a Turkish default
    // locale "APPLICATION/JSON" would lowercase to "applıcatıon/json".
    val previousDefaultLocale = java.util.Locale.getDefault
    try {
      java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
      assert(Rendering.select(None, Some("APPLICATION/JSON")) == Rendering.Json)
      assert(Rendering.select(None, Some("TEXT/HTML, APPLICATION/JSON")) == Rendering.Page)
    } finally {
      java.util.Locale.setDefault(previousDefaultLocale)
    }

    final case class VM(title: String)
    val vm = VM("Hi")
    val presentation = Presentation[VM](
      page = m => Html(s"<html><body><h1>${m.title}</h1></body></html>"),
      ioReplyFragment = m => Html(s"<h1>${m.title}</h1>"),
      json = Some(new ViewModelJson[VM] {
        def encode(model: VM): String = s"""{"title":"${model.title}"}"""
      })
    )
    assert(presentation.render(vm, Rendering.Page) ==
      Some(Rendered(Rendered.HtmlContentType, "<html><body><h1>Hi</h1></body></html>")))
    assert(presentation.render(vm, Rendering.IoReplyFragment) ==
      Some(Rendered(Rendered.HtmlContentType, "<h1>Hi</h1>")))
    assert(presentation.render(vm, Rendering.Json) ==
      Some(Rendered(Rendered.JsonContentType, "{\"title\":\"Hi\"}")))

    // A lambda converts to the single-method ViewModelJson trait directly.
    val samPresentation = Presentation[VM](
      page = m => Html(s"<p>${m.title}</p>"),
      ioReplyFragment = m => Html(s"<p>${m.title}</p>"),
      json = Some(m => s"""{"title":"${m.title}"}""")
    )
    assert(samPresentation.render(vm, Rendering.Json) == presentation.render(vm, Rendering.Json))

    val htmlOnly = Presentation[VM](
      page = m => Html(s"<p>${m.title}</p>"),
      ioReplyFragment = m => Html(s"<p>${m.title}</p>")
    )
    assert(htmlOnly.render(vm, Rendering.Json).isEmpty)
    assert(htmlOnly.render(vm, Rendering.Page).nonEmpty)
    assert(htmlOnly.render(vm, Rendering.IoReplyFragment).nonEmpty)

    val ok = IoReplyFragmentEnvelope.success(
      ioReplyFragments = Seq("feedback" -> IoReplyFragmentMarkup(Html("<p>Thanks</p>"))),
      variables = Seq("status" -> "done"),
      time = 1790131202.32
    )
    assert(ok == "{\"meta\":{\"code\":200,\"message\":\"OK\",\"time\":1790131202.32}," +
      "\"data\":{\"fragments\":{\"feedback\":{\"markup\":\"\\u003cp\\u003eThanks\\u003c/p\\u003e\"}}," +
      "\"variables\":{\"status\":\"done\"}}}", ok)

    val withAssets = IoReplyFragmentEnvelope.success(
      ioReplyFragments = Seq("list" -> IoReplyFragmentMarkup(
        Html("<ul></ul>"),
        js = Seq("paste.ui.pager"),
        css = Seq("paste.ui.pager")
      )),
      variables = Seq.empty,
      time = 1.5
    )
    assert(withAssets == "{\"meta\":{\"code\":200,\"message\":\"OK\",\"time\":1.5}," +
      "\"data\":{\"fragments\":{\"list\":{\"markup\":\"\\u003cul\\u003e\\u003c/ul\\u003e\"," +
      "\"js\":[\"paste.ui.pager\"],\"css\":[\"paste.ui.pager\"]}},\"variables\":{}}}", withAssets)

    assert(IoReplyFragmentEnvelope.failure(400, Some(IoReplyFragmentEnvelopeError(IoReplyFragmentEnvelopeErrorType.ParamError, "bad field")), 2.5) ==
      "{\"meta\":{\"code\":400,\"message\":\"Bad Request\",\"time\":2.5," +
        "\"error_type\":\"param_error\",\"error_code\":400,\"error_detail\":\"bad field\"},\"data\":{}}")
    assert(IoReplyFragmentEnvelope.failure(403, Some(IoReplyFragmentEnvelopeError(IoReplyFragmentEnvelopeErrorType.AuthorizationError)), 0.25) ==
      "{\"meta\":{\"code\":403,\"message\":\"Forbidden\",\"time\":0.25," +
        "\"error_type\":\"authorization_error\",\"error_code\":403,\"error_detail\":\"\"},\"data\":{}}")
    assert(IoReplyFragmentEnvelope.failure(422, Some(IoReplyFragmentEnvelopeError(IoReplyFragmentEnvelopeErrorType.ParamError)), 0.5) ==
      "{\"meta\":{\"code\":422,\"message\":\"Unprocessable Content\",\"time\":0.5," +
        "\"error_type\":\"param_error\",\"error_code\":400,\"error_detail\":\"\"},\"data\":{}}")
    assert(IoReplyFragmentEnvelope.failure(500, None, 3.25) ==
      "{\"meta\":{\"code\":500,\"message\":\"Internal Server Error\",\"time\":3.25},\"data\":{}}")
    assert(IoReplyFragmentEnvelope.failure(599, None, 1.5) ==
      "{\"meta\":{\"code\":599,\"message\":\"\",\"time\":1.5},\"data\":{}}")

    val escaped = IoReplyFragmentEnvelope.success(
      ioReplyFragments = Seq("f" -> IoReplyFragmentMarkup(Html("<b>&\"\\\n\u0001</b>"))),
      variables = Seq("k" -> "v\u2028w"),
      time = 0.5
    )
    assert(escaped == "{\"meta\":{\"code\":200,\"message\":\"OK\",\"time\":0.5}," +
      "\"data\":{\"fragments\":{\"f\":{\"markup\":\"\\u003cb\\u003e\\u0026\\\"\\\\\\n\\u0001\\u003c/b\\u003e\"}}," +
      "\"variables\":{\"k\":\"v\\u2028w\"}}}", escaped)

    assert(rejected(IoReplyFragmentEnvelope.success(Seq.empty, Seq.empty, Double.NaN)))
    assert(rejected(IoReplyFragmentEnvelope.success(Seq.empty, Seq.empty, Double.PositiveInfinity)))
    assert(rejected(IoReplyFragmentEnvelope.failure(500, None, Double.NegativeInfinity)))

    val fragment = IoReplyFragmentMarkup(Html("<p>x</p>"))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment, "a" -> fragment), Seq.empty, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("k" -> "1", "k" -> "2"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a b" -> fragment), Seq.empty, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("" -> fragment), Seq.empty, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq((null: String) -> fragment), Seq.empty, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("" -> "v"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("a b" -> "v"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("a,b" -> "v"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("a=b" -> "v"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq((null: String) -> "v"), 1.0)))
    assert(!rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("status" -> "done"), 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.failure(200, None, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.failure(302, None, 1.0)))
    assert(!rejected(IoReplyFragmentEnvelope.failure(400, None, 1.0)))
    assert(!rejected(IoReplyFragmentEnvelope.failure(599, None, 1.0)))
    assert(rejected(IoReplyFragmentEnvelopeError(null)))
    assert(rejected(IoReplyFragmentEnvelopeError(IoReplyFragmentEnvelopeErrorType.ParamError, null)))
    assert(rejected(IoReplyFragmentMarkup(null)))
    assert(rejected(IoReplyFragmentMarkup(Html("x"), js = Seq(null))))
    assert(rejected(IoReplyFragmentMarkup(Html("x"), css = Seq(null))))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> (null: IoReplyFragmentMarkup)), Seq.empty, 1.0)))
    assert(rejected(IoReplyFragmentEnvelope.success(
      Seq("a" -> fragment), Seq("k" -> (null: String)), 1.0)))

    // PagePresenter JSON goes through a caller-supplied encoder; the
    // inherited no-arg default and presenter failures both surface loudly.
    val named = new PagePresenter[String] {
      def present(): String = "home"
    }
    val stringJson: ViewModelJson[String] = model => s"""{"page":"$model"}"""
    assert(named.presentAsJson(stringJson) == "{\"page\":\"home\"}")
    val broken = new PagePresenter[String] {
      def present(): String = throw new IllegalStateException("boom")
    }
    assert(Try(broken.presentAsJson(stringJson)) match {
      case Failure(_: IllegalStateException) => true
      case _ => false
    })
    assert(Try(named.presentAsJson()) match {
      case Failure(_: UnsupportedOperationException) => true
      case _ => false
    })

    assert(IoReplyFragmentEnvelope.reasonPhrase(200) == "OK")
    assert(IoReplyFragmentEnvelope.reasonPhrase(204) == "No Content")
    assert(IoReplyFragmentEnvelope.reasonPhrase(302) == "Found")
    assert(IoReplyFragmentEnvelope.reasonPhrase(400) == "Bad Request")
    assert(IoReplyFragmentEnvelope.reasonPhrase(404) == "Not Found")
    assert(IoReplyFragmentEnvelope.reasonPhrase(422) == "Unprocessable Content")
    assert(IoReplyFragmentEnvelope.reasonPhrase(500) == "Internal Server Error")
    assert(IoReplyFragmentEnvelope.reasonPhrase(503) == "Service Unavailable")
    assert(IoReplyFragmentEnvelope.reasonPhrase(0) == "")
    assert(IoReplyFragmentEnvelope.reasonPhrase(599) == "")
  }
}
