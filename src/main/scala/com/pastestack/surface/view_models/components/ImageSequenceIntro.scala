package com.pastestack.surface.view_models.components

// The finite introductory playback the sequence owns: a declared range
// on a declared clock, with an authored skip unless skip is false —
// never a general autoplay. Field names mirror the module's
// data-sequence-intro-* attributes.
final case class ImageSequenceIntro(
  durationMs: Int,
  from: Option[Int] = None,
  to: Option[Int] = None,
  easing: IntroEasing = IntroEasing.EaseInOut,
  skipLabel: String = "Skip intro",
  bufferMs: Option[Int] = None,
  // The skip control is optional, as it is to the module: false emits no
  // button and no data-sequence-intro-skip, and scroll, a hidden
  // document, failure or completion still end the intro. Declared last,
  // so positional construction is unchanged.
  skip: Boolean = true
) {
  require(from != null && from.forall(_ >= 0), "ImageSequenceIntro from must be nonnegative")
  require(to != null && to.forall(_ >= 0), "ImageSequenceIntro to must be nonnegative")
  require(easing != null, "ImageSequenceIntro easing must not be null")
  require(bufferMs != null, "ImageSequenceIntro bufferMs must not be null")
  // A declared intro is a real intro: the module treats a zero duration as
  // disabling it, so the typed model refuses zero rather than emit intro
  // attributes and a dead skip control for a playback that never runs.
  // No intro is expressed as None on ImageSequence, not durationMs = 0.
  require(durationMs > 0, "ImageSequenceIntro durationMs must be positive")
  require(bufferMs.forall(_ >= 0), "ImageSequenceIntro bufferMs must be nonnegative")
  require(skipLabel != null, "ImageSequenceIntro skipLabel must not be null")
  // The label is checked only where it renders: an intro without a skip
  // emits no control to name. It must carry a character that draws — a
  // label of no-break, ideographic or em spaces, zero-width characters or
  // a byte-order mark passes a trim but leaves the button blank and
  // without an accessible name.
  require(!skip || ImageSequenceIntro.visibleLabel(skipLabel), "ImageSequenceIntro skipLabel must carry a visible character")
}

object ImageSequenceIntro {
  // Letters, numbers, punctuation and symbols draw; separators, controls,
  // format characters, combining marks on their own and private-use code
  // points do not, and neither do the Hangul fillers, the blank braille
  // pattern or the object replacement character, which are letters and
  // symbols that render as nothing. A code point this JDK's Unicode
  // tables do not assign counts as drawing: a newer engine may have a
  // glyph for it, and refusing a real label is the worse mistake. The
  // noncharacters and the reserved default-ignorable ranges stay blank,
  // since no Unicode version will draw them.
  private val Visible = Set[Int](
    Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
    Character.MODIFIER_LETTER, Character.OTHER_LETTER,
    Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER,
    Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
    Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
    Character.OTHER_PUNCTUATION,
    Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL,
    Character.UNASSIGNED
  )
  private val BlankGlyphs = Set(0x115f, 0x1160, 0x3164, 0xffa0, 0x2800, 0xfffc)

  private def neverDrawn(cp: Int): Boolean =
    (cp >= 0xfdd0 && cp <= 0xfdef) || (cp & 0xfffe) == 0xfffe ||
      (cp >= 0x2060 && cp <= 0x206f) || (cp >= 0xfff0 && cp <= 0xfff8) || (cp >= 0xe0000 && cp <= 0xe0fff)

  private[components] def visibleLabel(label: String): Boolean =
    label.codePoints().anyMatch(cp =>
      Visible.contains(Character.getType(cp)) && !BlankGlyphs.contains(cp) && !neverDrawn(cp)
    )
}
