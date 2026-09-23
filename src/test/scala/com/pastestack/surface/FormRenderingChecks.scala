package com.pastestack.surface

import com.pastestack.surface.view_models.components.IoReplyFragmentTarget
import com.pastestack.surface.view_models.components.formfield.{Button, Form, FormEnctype, FormMethod, Input, Textarea}
import play.twirl.api.Html

import scala.util.{Failure, Try}

object FormRenderingChecks {
  private def renderForm(form: Form, content: Html = Html("<p>Fields</p>")): String =
    paste.surface.components.formfield.html.form(form)(content).body

  private def renderIoReplyFragmentTarget(target: IoReplyFragmentTarget, content: Html = Html("<p>Inner</p>")): String =
    paste.surface.components.html.ioReplyFragmentTarget(target)(content).body

  private def rejected(f: => Any): Boolean =
    Try(f) match {
      case Failure(_: IllegalArgumentException) => true
      case _ => false
    }

  def main(args: Array[String]): Unit = {
    val plain = renderForm(Form("/feedback"))
    assert(plain.contains("class=\"paste-ui-form\""))
    assert(plain.contains("action=\"/feedback\""))
    assert(plain.contains("method=\"post\""))
    assert(plain.contains("enctype=\"application/x-www-form-urlencoded\""))
    assert(plain.contains("accept-charset=\"utf-8\""))
    assert(!plain.contains("data-paste-form"))
    assert(!plain.contains("id=\""))
    assert(!plain.contains("paste-ui-form-errors"))
    assert(plain.contains("<p>Fields</p>"))
    assert(plain.contains("</form>"))

    val full = renderForm(Form(
      action = "/signup",
      method = FormMethod.Get,
      enctype = FormEnctype.Multipart,
      id = Some("signup-form"),
      backgroundSubmit = true,
      errors = Seq("Try again", "Nope & nope"),
      cssClass = "wide",
      dataAttributes = Map("track" -> "signup")
    ))
    assert(full.contains("class=\"paste-ui-form wide\""))
    assert(full.contains("id=\"signup-form\""))
    assert(full.contains("action=\"/signup\""))
    assert(full.contains("method=\"get\""))
    assert(full.contains("enctype=\"multipart/form-data\""))
    assert(full.contains("data-paste-form"))
    assert(full.contains("data-track=\"signup\""))
    assert(full.contains("<ul class=\"paste-ui-form-errors\" role=\"alert\">"))
    assert(full.contains("<li>Try again</li>"))
    assert(full.contains("<li>Nope &amp; nope</li>"))

    val escaped = renderForm(Form("/feedback\" onmouseover=\"x"))
    assert(escaped.contains("action=\"/feedback&quot; onmouseover=&quot;x\""))
    assert(!escaped.contains("onmouseover=\"x\""))

    assert(rejected(Form(null)))
    assert(rejected(Form("")))
    assert(rejected(Form("   ")))
    assert(rejected(Form("/x", method = null)))
    assert(rejected(Form("/x", enctype = null)))
    assert(rejected(Form("/x").copy(action = "")))
    assert(rejected(Form("/x").copy(method = null)))
    assert(rejected(Form("/x").copy(enctype = null)))
    assert(rejected(Form("/x", dataAttributes = null)))
    assert(rejected(Form("/x", dataAttributes = Map("x onclick" -> "y"))))
    assert(rejected(Form("/x", dataAttributes = Map("a=b" -> "y"))))
    assert(rejected(Form("/x", dataAttributes = Map("" -> "y"))))
    assert(rejected(Form("/x", dataAttributes = Map("Upper" -> "y"))))
    assert(!rejected(Form("/x", method = FormMethod.Post, enctype = FormEnctype.Multipart)))
    assert(!rejected(Form("/x", dataAttributes = Map("umami-event" -> "v", "paste-autogrow" -> "true"))))
    assert(rejected(Input(name = "email", dataAttributes = Map("x onclick" -> "y"))))
    assert(rejected(Button(name = "go", label = "Go", dataAttributes = Map("a=b" -> "y"))))
    assert(rejected(Textarea(name = "bio", dataAttributes = Map("Bad Key" -> "y"))))

    val target = renderIoReplyFragmentTarget(IoReplyFragmentTarget("feedback"))
    assert(target.contains("class=\"paste-ui-io-reply-fragment-target\""))
    assert(target.contains("data-paste-io-reply-fragment-target=\"feedback\""))
    assert(target.contains("<p>Inner</p>"))
    assert(target.contains("</div>"))

    val styled = renderIoReplyFragmentTarget(IoReplyFragmentTarget("signup", cssClass = "compact"))
    assert(styled.contains("class=\"paste-ui-io-reply-fragment-target compact\""))
    assert(styled.contains("data-paste-io-reply-fragment-target=\"signup\""))

    assert(rejected(IoReplyFragmentTarget(null)))
    assert(rejected(IoReplyFragmentTarget("")))
    assert(rejected(IoReplyFragmentTarget("   ")))
    assert(rejected(IoReplyFragmentTarget("two words")))
    assert(rejected(IoReplyFragmentTarget("tab\tname")))
    assert(rejected(IoReplyFragmentTarget("feedback").copy(name = null)))
    assert(rejected(IoReplyFragmentTarget("feedback").copy(name = "a b")))
    assert(!rejected(IoReplyFragmentTarget("dash-name_ok")))
  }
}
