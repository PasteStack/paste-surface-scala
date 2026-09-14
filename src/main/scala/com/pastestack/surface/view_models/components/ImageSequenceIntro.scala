package com.pastestack.surface.view_models.components

final case class ImageSequenceIntro(
  toFrame: Option[Int] = None,
  durationMs: Int = 1600,
  mode: String = "return",
  trigger: String = "ready",
  easing: String = "ease-in-out",
  skipLabel: String = "Skip intro"
) {
  require(toFrame.forall(_ >= 0), "ImageSequenceIntro toFrame must be nonnegative")
  require(durationMs > 0, "ImageSequenceIntro durationMs must be positive")
  require(mode == "return" || mode == "hold", "ImageSequenceIntro mode must be return or hold")
  require(trigger == "ready" || trigger == "visible", "ImageSequenceIntro trigger must be ready or visible")
  require(easing == "linear" || easing == "ease-in-out", "ImageSequenceIntro easing must be linear or ease-in-out")
  require(Option(skipLabel).exists(_.trim.nonEmpty), "ImageSequenceIntro skipLabel must not be blank")
}
