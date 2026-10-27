package com.pastestack.surface

import java.util.regex.Pattern
import com.pastestack.surface.view_models.components.Marquee
import play.twirl.api.Html

object MarqueeRenderingChecks {
  private def render(model: Marquee, items: Seq[Html]): String =
    paste.surface.components.html.marquee(model)(items).body

  private def occurrences(html: String, literal: String): Int =
    Pattern.quote(literal).r.findAllMatchIn(html).length

  private def checkStructure(
    html: String,
    items: Seq[Html],
    escapedLabel: String = "Partners",
    escapedPauseLabel: String = "Pause motion",
    escapedResumeLabel: String = "Resume motion",
    duration: String = "45.0",
    escapedClass: String = "paste-ui-marquee"
  ): Unit = {
    val itemPattern = items.map { item =>
      Pattern.quote("<li class=\"paste-ui-marquee-item\">") +
        "\\s*" + Pattern.quote(item.body) + "\\s*</li>\\s*"
    }.mkString
    val pattern = "\\s*" +
      Pattern.quote(s"""<div class="$escapedClass"""") + "\\s+data-paste-marquee\\s+" +
      Pattern.quote(s"""data-paste-marquee-duration="$duration"""") + "\\s+" +
      Pattern.quote(s"""data-paste-marquee-pause-label="$escapedPauseLabel"""") + "\\s+" +
      Pattern.quote(s"""data-paste-marquee-resume-label="$escapedResumeLabel">""") + "\\s*" +
      Pattern.quote(s"""<ul class="paste-ui-marquee-items" role="list" aria-label="$escapedLabel">""") +
      "\\s*" + itemPattern + "</ul>\\s*" +
      Pattern.quote(s"""<button class="paste-ui-marquee-toggle" type="button" hidden disabled>$escapedPauseLabel</button>""") +
      "\\s*</div>\\s*"

    assert(html.matches(pattern), s"Unexpected marquee structure:\n$html")
    assert(occurrences(html, "class=\"paste-ui-marquee-item\"") == items.size)
    assert(occurrences(html, "class=\"paste-ui-marquee-items\"") == 1)
    assert(occurrences(html, "class=\"paste-ui-marquee-toggle\"") == 1)
    assert(!html.contains("aria-pressed"))
    assert(!html.contains("data-paste-marquee-state"))
    assert(!html.contains("<script"))
    assert(!html.contains(" style="))
    assert(!html.contains(" id="))
  }

  private def rejects(configuration: => Marquee): Unit = {
    val rejected = try {
      configuration
      false
    } catch {
      case _: IllegalArgumentException => true
    }
    assert(rejected, "Invalid marquee configuration must fail at the model boundary")
  }

  def main(args: Array[String]): Unit = {
    val defaults: Marquee = Marquee("Partners")
    assert(defaults.durationSeconds == 45.0)
    assert(defaults.pauseLabel == "Pause motion")
    assert(defaults.resumeLabel == "Resume motion")
    assert(defaults.cssClass.isEmpty)

    val items = Seq(
      Html("""<span data-slot="first"><strong>First &amp; trusted</strong></span>"""),
      Html("""<div data-slot="second">Second</div><span>Same slot</span>"""),
      Html("""<span data-slot="third">Third</span>""")
    )
    val rendered = render(defaults, items)
    checkStructure(rendered, items)
    items.foreach(item => assert(occurrences(rendered, item.body) == 1))
    assert(!rendered.contains("&lt;span"), "Trusted Html slots must not be escaped again")

    checkStructure(render(defaults, Seq.empty), Seq.empty)
    checkStructure(render(defaults, items.take(1)), items.take(1))
    val repeated = Seq(items.head, items.head, Html(""))
    checkStructure(render(defaults, repeated), repeated)
    assert(occurrences(render(defaults, repeated), items.head.body) == 2)

    val interactiveItems = Seq(
      Html("""<a href="/details">Details</a>"""),
      Html("""<form action="/search"><input name="query"><button type="submit">Search</button></form>""")
    )
    checkStructure(render(defaults, interactiveItems), interactiveItems)

    val special = """<b title="A&B">unsafe</b>"""
    val escaped = "&lt;b title=&quot;A&amp;B&quot;&gt;unsafe&lt;/b&gt;"
    val custom = Marquee(
      label = special,
      durationSeconds = 12.5,
      pauseLabel = "Pause " + special,
      resumeLabel = "Resume " + special,
      cssClass = """custom" onmouseover="<unsafe>&"""
    )
    val customHtml = render(custom, items)
    checkStructure(
      customHtml,
      items,
      escapedLabel = escaped,
      escapedPauseLabel = "Pause " + escaped,
      escapedResumeLabel = "Resume " + escaped,
      duration = "12.5",
      escapedClass = "paste-ui-marquee custom&quot; onmouseover=&quot;&lt;unsafe&gt;&amp;"
    )
    assert(!customHtml.contains(special))
    assert(!customHtml.contains(" onmouseover=\""))
    assert(occurrences(customHtml, "Pause " + escaped) == 2)
    assert(occurrences(customHtml, "Resume " + escaped) == 1)

    Seq("", " ", "\t\r\n", null).foreach { blank =>
      rejects(Marquee(blank))
      rejects(defaults.copy(pauseLabel = blank))
      rejects(defaults.copy(resumeLabel = blank))
    }
    Seq(0.0, -0.0, -1.0, Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity).foreach { duration =>
      rejects(defaults.copy(durationSeconds = duration))
    }
    Seq(java.lang.Double.MIN_VALUE, 0.5, Double.MaxValue).foreach { duration =>
      val model = defaults.copy(durationSeconds = duration)
      val expected = java.math.BigDecimal.valueOf(duration).toPlainString
      assert(!model.durationText.exists(c => c == 'e' || c == 'E'))
      assert(model.durationText.toDouble == duration)
      checkStructure(render(model, Seq.empty), Seq.empty, duration = expected)
    }
  }
}
