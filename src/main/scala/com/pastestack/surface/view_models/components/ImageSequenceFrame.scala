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

import scala.jdk.CollectionConverters._

// One catalog frame: the base representation plus the sharper variants a
// resolution upgrade may choose — ascending intrinsic width and height,
// same aspect, exactly the shape the inert JSON block carries. Supplied
// rungs may arrive unordered the way the page listed them; sortedUpgrades
// is the serialized order. A rung that is not both wider and taller than
// the one below it, a blank src and aspect disagreement beyond one
// percent are rejected at the boundary rather than written out.
final case class ImageSequenceFrame(
  src: String,
  width: Int,
  height: Int,
  upgrades: Seq[ImageRepresentation] = Seq.empty
) {
  require(ImageRepresentation.usableSrc(src), "ImageSequenceFrame src must not be blank or carry control characters")
  require(width > 0, "ImageSequenceFrame width must be positive")
  require(height > 0, "ImageSequenceFrame height must be positive")
  require(
    Option(upgrades).exists(_.forall(_ != null)),
    "ImageSequenceFrame upgrades must not be null"
  )

  val sortedUpgrades: Seq[ImageRepresentation] = upgrades.sortBy(_.width)
  // One resource, one intrinsic size, inside the frame: rung widths
  // ascend strictly, so a repeated source can only be one URL
  // claiming two dimensions — the declaration the Element's same-source
  // skip would strand a genuine sharper rung behind. Cross-frame source
  // reuse remains the catalog's own check.
  require(
    (src +: sortedUpgrades.map(_.src)).map(ImageRepresentation.sourceIdentity).distinct.size ==
      upgrades.size + 1,
    "ImageSequenceFrame representation sources must be distinct"
  )
  // The base is the lowest rung: the ladder must ascend strictly above
  // it in both width and height — the Element refuses a rung that grows
  // in one dimension only, which the aspect band alone would admit.
  sortedUpgrades.foldLeft((width, height)) { case ((previousWidth, previousHeight), upgrade) =>
    require(
      upgrade.width > previousWidth && upgrade.height > previousHeight,
      "ImageSequenceFrame upgrades must ascend in width and height above the base"
    )
    require(
      math.abs(upgrade.width.toDouble / upgrade.height - width.toDouble / height) <=
        0.01 * (width.toDouble / height),
      "ImageSequenceFrame upgrade aspect must match its frame within one percent"
    )
    (upgrade.width, upgrade.height)
  }

  private[surface] def jsonMap: java.util.Map[String, Object] = {
    // Insertion-ordered for byte-stable output across JVM runs.
    val map = new java.util.LinkedHashMap[String, Object]()
    map.put("src", src)
    map.put("width", Int.box(width))
    map.put("height", Int.box(height))
    map.put("upgrades", sortedUpgrades.map(_.jsonMap).asJava)
    map
  }
}
