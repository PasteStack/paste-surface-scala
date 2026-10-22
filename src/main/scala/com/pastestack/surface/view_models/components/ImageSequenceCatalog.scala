package com.pastestack.surface.view_models.components

import com.fasterxml.jackson.databind.ObjectMapper

import scala.jdk.CollectionConverters._

// The whole frame set a sequence samples: a non-empty list of frames in
// display order. toJson is the inert block the page embeds.
final case class ImageSequenceCatalog(frames: Seq[ImageSequenceFrame]) {
  require(
    Option(frames).exists(list => list.nonEmpty && list.forall(_ != null)),
    "ImageSequenceCatalog frames must not be empty"
  )

  // One resource carries exactly one intrinsic size: the module rejects a
  // catalog where the same source URL claims different dimensions, so the
  // typed model refuses it at construction. Sources compare by their
  // resolved identity (ImageRepresentation.sourceIdentity), so spellings
  // of one URL collide here as they do in the browser; an alias only the
  // real document base reveals is still the Element's to reject.
  frames
    .flatMap(frame =>
      (ImageRepresentation.sourceIdentity(frame.src), frame.width, frame.height) +:
        frame.sortedUpgrades.map(upgrade =>
          (ImageRepresentation.sourceIdentity(upgrade.src), upgrade.width, upgrade.height)
        )
    )
    .foldLeft(Map.empty[String, (Int, Int)]) { (seen, representation) =>
      val (src, width, height) = representation
      seen.get(src).foreach { case (priorWidth, priorHeight) =>
        require(
          priorWidth == width && priorHeight == height,
          s"ImageSequenceCatalog source '$src' carries conflicting dimensions " +
            s"${width}x${height} versus ${priorWidth}x${priorHeight}"
        )
      }
      seen + (src -> (width, height))
    }

  // The inert block the page embeds — explicit frame URLs, no logic.
  // The catalog is immutable, so it serializes once, not on every render.
  lazy val toJson: String =
    ImageSequenceCatalog.htmlSafeJson(
      java.util.Map.of("frames", frames.map(_.jsonMap).asJava)
    )
}

object ImageSequenceCatalog {
  // Lazy: the package object's aliases initialize this companion, and an
  // application that never serializes a catalog should not pay for Jackson.
  private lazy val mapper = new ObjectMapper()

  // JSON that goes into a <script type="application/json"> element must
  // be HTML-safe: every '<', '>', '&' and line separator a value could
  // carry is emitted as a unicode escape — a src reading literally
  // "</script>" can never terminate the data block. Catalogs and
  // standalone descriptors share the one serializer.
  private[surface] def htmlSafeJson(value: Object): String =
    mapper.writeValueAsString(value)
      .replace("<", "\\u003c")
      .replace(">", "\\u003e")
      .replace("&", "\\u0026")
      .replace(0x2028.toChar.toString, "\\u2028")
      .replace(0x2029.toChar.toString, "\\u2029")
}
