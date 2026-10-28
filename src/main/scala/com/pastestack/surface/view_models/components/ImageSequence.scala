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
  scrubBufferFrames: Option[Int] = None,
  playback: SequencePlayback = SequencePlayback.Direct
) {
  require(playback != null, "ImageSequence playback must not be null")
  playback match {
    case SequencePlayback.Queued(fps) => require(fps >= 1 && fps <= 120, "Queued playback rate must be 1..120 fps")
    case SequencePlayback.Direct => ()
  }
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
