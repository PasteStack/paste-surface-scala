package com.pastestack.surface.view_models.components

// The whole declaration an imagesequence host needs: which catalog,
// which interval the scroll range samples, the optional intro, and
// whether displayed frames may resolve sharper representations. Field
// names mirror the module's data-sequence-* attributes; presentation —
// poster, sizing, crop, reduced-motion choice — stays authored in CSS.
final case class ImageSequence(
  catalog: ImageSequenceCatalog,
  cssClass: String = "",
  scrub: ScrollScrub = ScrollScrub.TopExit,
  from: Option[Int] = None,
  to: Option[Int] = None,
  intro: Option[ImageSequenceIntro] = None,
  resolution: Boolean = true,
  scrubBufferFrames: Option[Int] = None
) {
  require(catalog != null, "ImageSequence catalog must not be null")
  require(cssClass != null, "ImageSequence cssClass must not be null")
  require(scrub != null, "ImageSequence scrub must not be null")
  require(intro != null && intro.forall(_ != null), "ImageSequence intro must not be null")
  require(from != null, "ImageSequence from must not be null")
  require(to != null, "ImageSequence to must not be null")
  require(
    scrubBufferFrames != null && scrubBufferFrames.forall(_ >= 0),
    "ImageSequence scrubBufferFrames must be nonnegative"
  )
  private def catalogIndex(value: Int, field: String): Unit =
    require(
      value >= 0 && value < catalog.frames.length,
      s"ImageSequence $field must be an index within the catalog"
    )
  from.foreach(value => catalogIndex(value, "from"))
  to.foreach(value => catalogIndex(value, "to"))
  intro.foreach { declared =>
    declared.from.foreach(value => catalogIndex(value, "intro from"))
    declared.to.foreach(value => catalogIndex(value, "intro to"))
  }
}
