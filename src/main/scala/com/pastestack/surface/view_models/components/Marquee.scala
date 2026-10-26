package com.pastestack.surface.view_models.components

final case class Marquee(
  label: String,
  durationSeconds: Double = 45.0,
  pauseLabel: String = "Pause motion",
  resumeLabel: String = "Resume motion",
  cssClass: String = ""
) {
  require(Option(label).exists(_.trim.nonEmpty), "Marquee label must not be blank")
  require(Option(pauseLabel).exists(_.trim.nonEmpty), "Marquee pauseLabel must not be blank")
  require(Option(resumeLabel).exists(_.trim.nonEmpty), "Marquee resumeLabel must not be blank")
  // A NaN comparison is already false, so `> 0.0` rejects NaN as well.
  require(
    durationSeconds > 0.0 && durationSeconds.isFinite,
    "Marquee durationSeconds must be positive and finite"
  )

  // The Elements declaration accepts decimal digits, not exponent notation.
  val durationText: String = java.math.BigDecimal.valueOf(durationSeconds).toPlainString
}
