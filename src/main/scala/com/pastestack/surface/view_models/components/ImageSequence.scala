package com.pastestack.surface.view_models.components

final case class ImageSequence(
  source: String,
  posterFirst: String,
  posterFinal: String,
  posterWidth: Int,
  posterHeight: Int,
  progressTargetId: Option[String] = None,
  range: String = "top-exit",
  preload: String = "auto",
  priority: Int = 0,
  cssClass: String = "",
  intro: Option[ImageSequenceIntro] = None,
  posterSources: Seq[ImageSequencePoster] = Seq.empty,
  posterSizes: String = "100vw",
  scrollFrom: Option[Int] = None,
  scrollTo: Option[Int] = None,
  fit: String = "cover",
  scrollWindowStart: Double = 0.0,
  scrollWindowEnd: Double = 1.0
) {
  private def requiredUrl(value: String, field: String): Unit =
    require(Option(value).exists(_.trim.nonEmpty), s"ImageSequence $field must not be blank")

  requiredUrl(source, "source")
  requiredUrl(posterFirst, "posterFirst")
  requiredUrl(posterFinal, "posterFinal")
  require(posterWidth > 0, "ImageSequence posterWidth must be positive")
  require(posterHeight > 0, "ImageSequence posterHeight must be positive")
  require(range == "top-exit" || range == "enter-exit", "ImageSequence range must be top-exit or enter-exit")
  require(preload == "auto" || preload == "all", "ImageSequence preload must be auto or all")
  require(priority >= 0, "ImageSequence priority must be nonnegative")
  require(scrollFrom.forall(_ >= 0), "ImageSequence scrollFrom must be nonnegative")
  require(scrollTo.forall(_ >= 0), "ImageSequence scrollTo must be nonnegative")
  require(scrollFrom.isEmpty == scrollTo.isEmpty, "ImageSequence scroll endpoints must be provided together")
  // The Element enforces the same contract at bind: an unknown fit throws, as
  // does a window whose end does not exceed its start.
  require(fit == "cover" || fit == "contain", "ImageSequence fit must be cover or contain")
  require(scrollWindowStart >= 0.0 && scrollWindowStart <= 1.0, "ImageSequence scrollWindowStart must be within 0 and 1")
  require(scrollWindowEnd >= 0.0 && scrollWindowEnd <= 1.0, "ImageSequence scrollWindowEnd must be within 0 and 1")
  require(scrollWindowEnd > scrollWindowStart, "ImageSequence scrollWindowEnd must exceed scrollWindowStart")
  progressTargetId.foreach { id =>
    require(id.trim.nonEmpty, "ImageSequence progressTargetId must not be blank")
    require(!id.contains("#") && !id.exists(_.isWhitespace), "ImageSequence progressTargetId must be a plain HTML ID")
  }
  // Candidate order is the renderer's business — it emits them ascending — so the
  // model asks only that each width name a distinct candidate. Two candidates of
  // the same width would put two srcset entries in the same slot, and which one a
  // browser picks is not something the caller decides.
  require(
    posterSources.map(_.width).distinct.size == posterSources.size,
    "ImageSequence poster widths must be distinct"
  )
  posterSources.headOption.foreach { first =>
    val expectedRatio = first.width.toDouble / first.height
    posterSources.foreach { poster =>
      // Rendered poster widths are rounded to whole pixels, so equal-looking
      // ratios differ in the last decimals; compare within a one percent band
      // rather than by exact equality.
      val ratio = poster.width.toDouble / poster.height
      require(
        math.abs(ratio - expectedRatio) <= expectedRatio * 0.01,
        "ImageSequence poster aspect ratios must match"
      )
    }
  }
}
