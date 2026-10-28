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
