package com.pastestack.surface

import java.util.regex.Pattern
import play.twirl.api.Html

/**
 * The renderer's half of the image-sequence contract: a poster that is readable
 * on its own, plus the configuration the Element reads. Frame expansion, canvas
 * pixels and scroll subscription belong to the Element, so anything resembling a
 * frame URL or runtime state in this markup is a defect.
 */
object ImageSequenceRenderingChecks {
  private val posters = Seq(
    ImageSequencePoster(960, 540, "/seq/960/first.webp", "/seq/960/final.webp", Some("/seq/960/first.jpg"), Some("/seq/960/final.jpg")),
    ImageSequencePoster(480, 270, "/seq/480/first.webp", "/seq/480/final.webp"),
    ImageSequencePoster(768, 432, "/seq/768/first.webp", "/seq/768/final.webp")
  )

  private def sequence(): ImageSequence = ImageSequence(
    source = "/sequences/hero",
    posterFirst = "/sequences/hero/first.jpg",
    posterFinal = "/sequences/hero/final.jpg",
    posterWidth = 1920,
    posterHeight = 1080
  )

  private def render(model: ImageSequence, mediaId: String = "main-image", progressId: String = "main"): String =
    paste.surface.components.html.imagesequence(model, mediaId, progressId).body

  private def occurrences(html: String, literal: String): Int =
    Pattern.quote(literal).r.findAllMatchIn(html).length

  private def rejects(configuration: => Any): Unit = {
    val rejected = try { configuration; false } catch { case _: IllegalArgumentException => true }
    assert(rejected, "Invalid image sequence configuration must fail at the model boundary")
  }

  def main(args: Array[String]): Unit = {
    val plain = render(sequence())
    assert(plain.contains("""id="main-image""""), plain)
    assert(plain.contains("data-paste-candidate-imagesequence"), plain)
    assert(plain.contains("""data-sequence-source="/sequences/hero""""), plain)
    assert(plain.contains("""data-sequence-progress-id="main""""), plain)
    assert(plain.contains("""data-sequence-range="top-exit""""), plain)
    assert(plain.contains("""data-sequence-preload="auto""""), plain)
    // Fit and the scroll window always carry the effective value; the model
    // defaults are the Element's defaults, so an element omitting them means
    // the same thing.
    assert(plain.contains("""data-sequence-fit="cover""""), plain)
    assert(plain.contains("""data-sequence-scroll-window-start="0.0""""), plain)
    assert(plain.contains("""data-sequence-scroll-window-end="1.0""""), plain)
    // Absent configuration stays absent rather than rendering an empty attribute.
    assert(!plain.contains("data-sequence-scroll-from"), plain)
    assert(!plain.contains("data-sequence-scroll-to"), plain)
    assert(!plain.contains("data-sequence-intro-mode"), plain)
    assert(!plain.contains("data-sequence-intro-skip"), plain)
    // The poster reserves layout and stays out of the accessibility tree.
    assert(plain.contains("""width="1920""""), plain)
    assert(plain.contains("""height="1080""""), plain)
    assert(plain.contains("""alt="""""), plain)
    assert(occurrences(plain, "<picture") == 1, plain)
    assert(!plain.contains("<script"), plain)
    assert(!plain.contains("data-sequence-state"), plain)

    // Poster candidates are emitted in ascending width whatever order they arrive in.
    val responsive = render(sequence().copy(posterSources = posters, posterSizes = "50vw"))
    val webpFirst = """<source type="image/webp" srcset="([^"]+)" sizes="50vw">""".r
      .findFirstMatchIn(responsive).map(_.group(1)).getOrElse(sys.error(responsive))
    assert(webpFirst == "/seq/480/first.webp 480w, /seq/768/first.webp 768w, /seq/960/first.webp 960w", webpFirst)
    val reducedMotion = """<source type="image/webp" media="\(prefers-reduced-motion: reduce\)" srcset="([^"]+)"""".r
      .findFirstMatchIn(responsive).map(_.group(1)).getOrElse(sys.error(responsive))
    assert(reducedMotion.contains("/seq/480/final.webp 480w"), reducedMotion)
    // Two jpeg sources — reduced-motion final frames, then first frames — and only
    // the one candidate that actually supplies fallbacks appears in either.
    val jpeg = """<source type="image/jpeg"[^>]*srcset="([^"]+)"""".r.findAllMatchIn(responsive).map(_.group(1)).toSeq
    assert(jpeg == Seq("/seq/960/final.jpg 960w", "/seq/960/first.jpg 960w"), jpeg.toString)

    val intro = render(sequence().copy(
      intro = Some(ImageSequenceIntro(toFrame = Some(45), durationMs = 3400, mode = "hold")),
      scrollFrom = Some(0),
      scrollTo = Some(60)
    ))
    assert(intro.contains("""data-sequence-intro-mode="hold""""), intro)
    assert(intro.contains("""data-sequence-intro-duration-ms="3400""""), intro)
    assert(intro.contains("""data-sequence-intro-to-frame="45""""), intro)
    assert(intro.contains("""data-sequence-scroll-from="0""""), intro)
    assert(intro.contains("""data-sequence-scroll-to="60""""), intro)
    assert(intro.contains("data-sequence-intro-skip"), intro)
    assert(intro.contains("hidden"), intro)

    val windowed = render(sequence().copy(
      fit = "contain",
      scrollWindowStart = 0.3,
      scrollWindowEnd = 0.65
    ))
    assert(windowed.contains("""data-sequence-fit="contain""""), windowed)
    assert(windowed.contains("""data-sequence-scroll-window-start="0.3""""), windowed)
    assert(windowed.contains("""data-sequence-scroll-window-end="0.65""""), windowed)

    // Model strings are escaped, never turned into new attributes.
    val escaped = render(sequence().copy(cssClass = """dark" onload="alert(1)"""))
    assert(!escaped.contains(""" onload=""""), escaped)
    assert(escaped.contains("paste-candidate-imagesequence dark&quot;"), escaped)

    val base = sequence()
    rejects(base.copy(source = ""))
    rejects(base.copy(posterFirst = " "))
    rejects(base.copy(posterFinal = null))
    rejects(base.copy(posterWidth = 0))
    rejects(base.copy(posterHeight = -1))
    rejects(base.copy(range = "middle"))
    rejects(base.copy(preload = "some"))
    rejects(base.copy(priority = -1))
    rejects(base.copy(progressTargetId = Some("#hero")))
    rejects(base.copy(progressTargetId = Some("hero section")))
    rejects(base.copy(progressTargetId = Some(" ")))
    rejects(base.copy(scrollFrom = Some(0)))
    rejects(base.copy(scrollTo = Some(60)))
    rejects(base.copy(scrollFrom = Some(-1), scrollTo = Some(60)))
    rejects(base.copy(fit = "fill"))
    rejects(base.copy(scrollWindowStart = -0.1))
    rejects(base.copy(scrollWindowStart = 1.1))
    rejects(base.copy(scrollWindowEnd = -0.5))
    rejects(base.copy(scrollWindowEnd = 1.5))
    rejects(base.copy(scrollWindowStart = 0.5, scrollWindowEnd = 0.5))
    rejects(base.copy(scrollWindowStart = 0.6, scrollWindowEnd = 0.4))
    rejects(base.copy(posterSources = Seq(posters.head, posters.head)))
    rejects(base.copy(posterSources = posters :+ ImageSequencePoster(480, 270, "/dup.webp", "/dupf.webp")))
    rejects(base.copy(posterSources = Seq(
      ImageSequencePoster(480, 270, "/a.webp", "/b.webp"),
      ImageSequencePoster(960, 300, "/c.webp", "/d.webp")
    )))
    rejects(ImageSequenceIntro(durationMs = 0))
    rejects(ImageSequenceIntro(toFrame = Some(-1)))
    rejects(ImageSequenceIntro(mode = "loop"))
    rejects(ImageSequenceIntro(trigger = "hover"))
    rejects(ImageSequenceIntro(easing = "bounce"))
    rejects(ImageSequenceIntro(skipLabel = " "))
    rejects(ImageSequencePoster(0, 270, "/a.webp", "/b.webp"))
    rejects(ImageSequencePoster(480, 270, "", "/b.webp"))

    // Rounded responsive widths of one design ratio are accepted.
    ImageSequence(
      source = "/s", posterFirst = "/f.jpg", posterFinal = "/l.jpg", posterWidth = 16, posterHeight = 9,
      posterSources = Seq(
        ImageSequencePoster(480, 270, "/480.webp", "/480f.webp"),
        ImageSequencePoster(1067, 600, "/1067.webp", "/1067f.webp")
      )
    )

    // A hero hands its media to the sequence: no parallax, no legacy image element.
    val heroHtml = paste.surface.components.html.hero(
      Hero("main", showImage = true, imageSequence = Some(sequence()))
    )(Html("<h1>Welcome</h1>")).body
    assert(heroHtml.contains("data-paste-candidate-imagesequence"), heroHtml)
    assert(heroHtml.contains("""id="main-image""""), heroHtml)
    assert(heroHtml.contains("""data-sequence-progress-id="main""""), heroHtml)
    assert(!heroHtml.contains("data-paste-parallax"), heroHtml)
    assert(!heroHtml.contains("paste-ui-hero-image"), heroHtml)
    assert(heroHtml.contains("<h1>Welcome</h1>"), heroHtml)

    val heroWithoutSequence = paste.surface.components.html.hero(
      Hero("main", showImage = true)
    )(Html("<h1>Welcome</h1>")).body
    assert(heroWithoutSequence.contains("data-paste-parallax"), heroWithoutSequence)
    assert(heroWithoutSequence.contains("paste-ui-hero-image"), heroWithoutSequence)
  }
}
