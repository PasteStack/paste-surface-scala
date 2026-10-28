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

// The scrub relationship an imagesequence or scrollscrub scope declares:
// anchor pairs name which subject edge meets which root edge at the
// range's ends, and the window narrows the span the way the page authored
// it — the geometry the module's data-scrub-* attributes carry verbatim.
// mode mirrors the module's container mode; root and subject are
// caller-owned selectors; absent means the module's own default.
final case class ScrollScrub(
  start: String = "top bottom",
  end: String = "bottom top",
  windowStart: Option[BigDecimal] = None,
  windowEnd: Option[BigDecimal] = None,
  mode: Option[ScrubMode] = None,
  root: Option[String] = None,
  subject: Option[String] = None
) {
  private def anchorPair(value: String, field: String): Unit = {
    val tokens = Option(value).fold(Seq.empty[String])(_.trim.split("\\s+").toSeq)
    require(
      tokens.length == 2 && tokens.forall(Set("top", "center", "bottom").contains),
      s"ScrollScrub $field must be two of top, center or bottom"
    )
  }
  anchorPair(start, "start")
  anchorPair(end, "end")
  require(
    windowStart != null && windowStart.forall(_ != null),
    "ScrollScrub windowStart must not contain null"
  )
  require(
    windowEnd != null && windowEnd.forall(_ != null),
    "ScrollScrub windowEnd must not contain null"
  )
  require(mode != null && mode.forall(_ != null), "ScrollScrub mode must not contain null")
  require(
    windowStart.isDefined == windowEnd.isDefined,
    "ScrollScrub window requires both start and end"
  )
  (windowStart, windowEnd) match {
    case (Some(a), Some(b)) =>
      // The Element parses each bound as a double: a bound must survive
      // that round trip exactly, or it can pass here and fail there (a
      // sub-ulp gap) or expand into an enormous plain-string attribute.
      require(
        Seq(a, b).forall(v => BigDecimal.decimal(v.toDouble) == v),
        "ScrollScrub window bounds must be exactly representable as doubles"
      )
      require(a >= 0 && b <= 1 && a < b, "ScrollScrub window requires 0 <= start < end <= 1")
    case _ => ()
  }
  require(
    root != null && root.forall(v => Option(v).exists(_.trim.nonEmpty)),
    "ScrollScrub root must not be blank"
  )
  require(
    subject != null && subject.forall(v => Option(v).exists(_.trim.nonEmpty)),
    "ScrollScrub subject must not be blank"
  )
}

object ScrollScrub {
  // The subject enters at the viewport bottom and leaves at the top —
  // the module's declared defaults, spelled out for call sites.
  val EnterExit: ScrollScrub = ScrollScrub()
  // The subject's top travels from the viewport top to its bottom's
  // arrival — the sticky hero's traverse.
  val TopExit: ScrollScrub = ScrollScrub(start = "top top", end = "bottom top")
}
