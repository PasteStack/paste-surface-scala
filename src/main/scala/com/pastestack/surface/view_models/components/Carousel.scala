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

case class CarouselSlide(
  content: String = "",
  cssClass: String = ""
)

final case class Carousel(
  name: String,
  autoCycleMs: Int = 0,
  showIndicators: Boolean = false,
  cssClass: String = "",
  variableWidth: Boolean = false,
  showCounter: Boolean = false,
  broadcastMoves: Boolean = false,
  animationEnabled: Boolean = false,
  controlsParentId: Option[String] = None,
  previousLabel: String = "Previous",
  nextLabel: String = "Next",
  pauseLabel: String = "Pause"
) {
  require(name != null, "Carousel name must not be null")
  require(autoCycleMs >= 0, "Carousel autoCycleMs must be nonnegative")
  require(controlsParentId != null, "Carousel controlsParentId must not be null")
  controlsParentId.foreach(GeneratedId.requireHtmlId("Carousel controlsParentId", _))
  Seq(previousLabel, nextLabel, pauseLabel).foreach { label =>
    require(Option(label).exists(_.trim.nonEmpty), "Carousel control labels must be nonblank")
  }
}
