// Copyright (c) 2017–present Tomshley LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pastestack.surface.view_model_factories

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.pastestack.surface.view_models.components.{ImageRepresentation, ImageSequenceCatalog, ImageSequenceFrame}

import scala.jdk.CollectionConverters._

// The version-1 adaptive/tiered manifest is a producer format: several
// `sizes[]` tiers of the same frames, smallest first in intent. This
// adapter parses it into ordered, validated, base-independent tiers and
// maps them into the canonical ImageSequenceCatalog — the only catalog
// shape the Element ever reads. URL resolution is caller policy passed
// to `toCatalog`; the adapter knows nothing about origins, CDNs,
// classpaths, or caches. Members other than schemaVersion, count, sizes,
// width, height, frames and src are ignored.
final class AdaptiveImageSequenceManifest private (
    declaredCount: Int,
    tiers: Vector[AdaptiveImageSequenceManifest.Tier]
) {
  // One catalog frame per declared index: the narrowest tier is each
  // frame's base representation, every wider tier one upgrade rung —
  // strictly ascending, the constraint the typed frame enforces again.
  def toCatalog(resolveSource: String => String): ImageSequenceCatalog = {
    require(resolveSource != null, "resolveSource must not be null")
    val base = tiers.head
    val wider = tiers.tail
    ImageSequenceCatalog((0 until declaredCount).map { index =>
      ImageSequenceFrame(
        src = resolveSource(base.frameSources(index)),
        width = base.width,
        height = base.height,
        upgrades = wider.map(tier =>
          ImageRepresentation(resolveSource(tier.frameSources(index)), tier.width, tier.height)
        )
      )
    })
  }
}

object AdaptiveImageSequenceManifest {

  private final case class Tier(width: Int, height: Int, frameSources: Vector[String])

  // The named, stable rejection every invalid source raises — never a
  // NullPointerException or a parser's raw message leaking through.
  final class InvalidManifest(message: String) extends IllegalArgumentException(message)

  private def invalid(message: String): Nothing = throw new InvalidManifest(message)

  // A manifest is exactly one unambiguous JSON document. Jackson's
  // defaults permit trailing roots and silently overwrite duplicate
  // object fields; either makes producer metadata ambiguous.
  private lazy val mapper = new ObjectMapper()
    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)

  private def required(node: JsonNode, field: String, what: String): JsonNode =
    Option(node).flatMap(n => Option(n.get(field)))
      .getOrElse(invalid(s"adaptive image sequence manifest $what is missing '$field'"))

  private def positiveInt(node: JsonNode, field: String, what: String): Int = {
    val value = required(node, field, what)
    if (!value.isInt || value.asInt <= 0)
      invalid(s"adaptive image sequence manifest $what carries an invalid '$field'")
    value.asInt
  }

  // The parser is stateless: a rejected document raises before the
  // value exists, so nothing partial can be cached or observed.
  def parseJson(json: String): AdaptiveImageSequenceManifest = {
    if (json == null || json.isBlank)
      invalid("adaptive image sequence manifest is absent or blank")
    val document = try mapper.readTree(json) catch {
      case e: JsonProcessingException =>
        throw new InvalidManifest("adaptive image sequence manifest is not valid JSON").initCause(e)
    }
    if (document == null || !document.isObject)
      invalid("adaptive image sequence manifest must be a JSON object")
    val version = required(document, "schemaVersion", "document")
    if (!version.isInt || version.asInt != 1)
      invalid(s"adaptive image sequence manifest carries an unsupported 'schemaVersion'")
    val declared = positiveInt(document, "count", "document")
    val sizesNode = required(document, "sizes", "document")
    if (!sizesNode.isArray)
      invalid("adaptive image sequence manifest 'sizes' is not an array")
    val sizes = sizesNode.elements().asScala.toVector
    if (sizes.isEmpty)
      invalid("adaptive image sequence manifest carries an empty 'sizes' array")
    val tiers = sizes.map { tier =>
      val width = positiveInt(tier, "width", "tier")
      val height = positiveInt(tier, "height", "tier")
      val framesNode = required(tier, "frames", "tier")
      if (!framesNode.isArray)
        invalid("adaptive image sequence manifest tier 'frames' is not an array")
      val frames = framesNode.elements().asScala.toVector
      if (frames.isEmpty)
        invalid("adaptive image sequence manifest tier carries an empty 'frames' array")
      val sources = frames.map { frame =>
        if (frame == null || !frame.isObject)
          invalid("adaptive image sequence manifest frame is not an object")
        val src = required(frame, "src", "frame")
        if (!src.isTextual || !ImageRepresentation.usableSrc(src.asText))
          invalid("adaptive image sequence manifest frame 'src' is not a non-blank string free of control characters")
        src.asText
      }
      if (sources.length != declared)
        invalid("adaptive image sequence manifest tiers must share one frame count and the declared 'count'")
      Tier(width, height, sources)
    }
    if (tiers.map(_.width).distinct.length != tiers.length)
      invalid("adaptive image sequence manifest carries a duplicate tier width")
    val sorted = tiers.sortBy(_.width)
    // Tier geometry does not depend on the caller's resolver: validate the
    // ladder once here with the typed frame's own rules, so an aspect or
    // ascent defect surfaces as InvalidManifest at parse, never at render.
    try ImageSequenceFrame(
      "tier-0", sorted.head.width, sorted.head.height,
      sorted.tail.zipWithIndex.map { case (tier, index) =>
        ImageRepresentation(s"tier-${index + 1}", tier.width, tier.height)
      }
    ) catch {
      case e: IllegalArgumentException =>
        throw new InvalidManifest(
          s"adaptive image sequence manifest tiers do not form a valid ladder: ${e.getMessage}"
        ).initCause(e)
    }
    // A frame src repeated across tiers — at the same frame or another —
    // names one resource at two sizes. The comparison is on the src's
    // resolved identity, not its spelling: "./480/f1.webp" and
    // "480/f1.webp" are one URL to the browser, which would refuse the
    // catalog the resolver builds from them.
    val tierSources = sorted.flatMap(_.frameSources.map(ImageRepresentation.sourceIdentity).distinct)
    if (tierSources.distinct.length != tierSources.length)
      invalid("adaptive image sequence manifest repeats a frame src across tiers")
    new AdaptiveImageSequenceManifest(declared, sorted)
  }
}
