package com.pastestack.surface

import com.pastestack.surface.view_models.components.{IoReplyFragmentTarget, IoReplyFragmentFormat, IoReplyFragmentLink}
import com.pastestack.surface.rendering.{IoReplyFragmentEnvelope, IoReplyFragmentMarkup, Rendering}
import play.twirl.api.Html
import scala.util.Try

object FragmentRenderingChecks {
  def main(args: Array[String]): Unit = {
    val target = paste.surface.components.html.ioReplyFragmentTarget(IoReplyFragmentTarget("article"))(Html("Current")).body
    assert(target.contains("data-paste-io-reply-fragment-target=\"article\""))
    assert(target.contains("data-paste-io-reply-fragment-format=\"html\""))
    val jsonTarget = paste.surface.components.html.ioReplyFragmentTarget(IoReplyFragmentTarget("article", format = IoReplyFragmentFormat.Json))(Html("Current")).body
    assert(jsonTarget.contains("data-paste-io-reply-fragment-format=\"json\""))
    val link = paste.surface.components.html.ioReplyFragmentLink(IoReplyFragmentLink("/article?a=1&b=2", "article"))(Html("Read")).body
    assert(link.contains("href=\"/article?a=1&amp;b=2\""))
    assert(link.contains("data-paste-io-reply-fragment=\"article\""))
    assert(!link.contains("onclick") && !link.contains("id="))
    assert(Try(IoReplyFragmentLink("", "article")).isFailure)
    assert(Try(IoReplyFragmentLink("/article", "bad name")).isFailure)
    assert(Try(IoReplyFragmentTarget("article", format = null)).isFailure)
    assert(Try(IoReplyFragmentTarget("article").copy(format = null)).isFailure)
    val reply = IoReplyFragmentEnvelope.render(422,
      Seq("article" -> IoReplyFragmentMarkup(Html("<p>Invalid</p>"))), Seq("status" -> "invalid"), 1.0, Some("Title \"quoted\""))
    assert(reply == """{"meta":{"code":422,"message":"Unprocessable Content","time":1.0},"data":{"fragments":{"article":{"markup":"\u003cp\u003eInvalid\u003c/p\u003e"}},"variables":{"status":"invalid"},"title":"Title \"quoted\""}}""")
    assert(IoReplyFragmentEnvelope.render(200, Seq.empty, Seq.empty, 1.0) == IoReplyFragmentEnvelope.success(Seq.empty, Seq.empty, 1.0))
    Seq(204, 205, 304).foreach(status => assert(Try(IoReplyFragmentEnvelope.render(status, Seq.empty, Seq.empty, 1.0)).isFailure))
    assert(Try(IoReplyFragmentEnvelope.render(0, Seq.empty, Seq.empty, 1.0)).isFailure)
    assert(Try(IoReplyFragmentEnvelope.render(600, Seq.empty, Seq.empty, 1.0)).isFailure)
    assert(Try(IoReplyFragmentEnvelope.render(200, Seq.empty, Seq.empty, 1.0, null)).isFailure)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("text/html, application/json")) == Rendering.IoReplyFragment)
    assert(Rendering.select(Some("XMLHttpRequest"), Some("application/json")) == Rendering.Json)
  }
}
