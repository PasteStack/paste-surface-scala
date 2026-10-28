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

final case class Marquee(
  label: String,
  durationSeconds: Double = 45.0,
  pauseLabel: String = "Pause motion",
  resumeLabel: String = "Resume motion",
  cssClass: String = ""
) {
  require(Option(label).exists(_.trim.nonEmpty), "Marquee label must not be blank")
  require(Option(pauseLabel).exists(_.trim.nonEmpty), "Marquee pauseLabel must not be blank")
  require(Option(resumeLabel).exists(_.trim.nonEmpty), "Marquee resumeLabel must not be blank")
  // A NaN comparison is already false, so `> 0.0` rejects NaN as well.
  require(
    durationSeconds > 0.0 && durationSeconds.isFinite,
    "Marquee durationSeconds must be positive and finite"
  )

  // The Elements declaration accepts decimal digits, not exponent notation.
  val durationText: String = java.math.BigDecimal.valueOf(durationSeconds).toPlainString
}
