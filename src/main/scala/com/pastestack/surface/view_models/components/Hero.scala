package com.pastestack.surface.view_models.components

final case class Hero(
  name: String,
  noContent: Boolean = false,
  showImage: Boolean = false,
  disableParallax: Boolean = false,
  stickyNavTarget: Boolean = false,
  imageSequence: Option[ImageSequence] = None
) {
  require(
    imageSequence != null && imageSequence.forall(_ != null),
    "Hero imageSequence must not contain null"
  )
  // The name becomes the section id, a fragment target resolved by exact
  // id. A sequenced hero also derives "#{name}-image-catalog" and, with an
  // intro skip, "#{name}-image-skip" — selectors the Element parses — so
  // only then must the name be a plain identifier.
  if (imageSequence.isDefined) GeneratedId.require("Hero name", name)
  else GeneratedId.requireFragmentId("Hero name", name)
}
