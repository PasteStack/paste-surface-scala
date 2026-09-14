package com.pastestack.surface.view_models.components

final case class ImageSequencePoster(
  width: Int,
  height: Int,
  first: String,
  finalFrame: String,
  fallbackFirst: Option[String] = None,
  fallbackFinal: Option[String] = None
) {
  require(width > 0 && height > 0, "ImageSequencePoster dimensions must be positive")
  require(Option(first).exists(_.trim.nonEmpty), "ImageSequencePoster first must not be blank")
  require(Option(finalFrame).exists(_.trim.nonEmpty), "ImageSequencePoster finalFrame must not be blank")
}
