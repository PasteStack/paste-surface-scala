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

package com.pastestack.surface.view_models.components

// One standalone progressively resolved image: an authored img baseline
// plus the inert descriptor the element reads for sharper rungs. The
// descriptor is one frame's worth of representation data — the same
// ascending-width, same-aspect ladder a catalog frame validates — so
// the typed frame carries the rung rules, not a second implementation.
// This is not a media framework: an ordinary img whose srcset or
// picture already governs needs no opt-in, and sequence hosts are owned
// by the sequence.
final case class ProgressiveImage(
  mediaId: MediaId,
  base: ImageRepresentation,
  alt: String,
  upgrades: Seq[ImageRepresentation] = Seq.empty,
  cssClass: String = ""
) {
  require(mediaId != null, "ProgressiveImage mediaId must not be null")
  require(base != null, "ProgressiveImage base must not be null")
  require(alt != null, "ProgressiveImage alt must not be null")
  require(cssClass != null, "ProgressiveImage cssClass must not be null")
  require(
    upgrades != null && upgrades.forall(_ != null),
    "ProgressiveImage upgrades must not be null"
  )

  // The descriptor mirrors one catalog frame exactly: base source and
  // intrinsic dimensions plus ascending rungs. Validating it through
  // ImageSequenceFrame keeps one rule set — a rung at or under the base
  // width or off the base aspect fails here the way it would in a
  // catalog.
  private val descriptor =
    ImageSequenceFrame(base.src, base.width, base.height, upgrades)

  // The inert block the page embeds — the same HTML-safe serialization
  // the catalog uses.
  lazy val descriptorJson: String =
    ImageSequenceCatalog.htmlSafeJson(descriptor.jsonMap)
}
