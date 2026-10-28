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

import com.pastestack.surface.view_models.components.Hero

/**
 * Factory for creating Hero ViewModels.
 */
object HeroFactory {
  
  def forHomePage(name: String = "main"): Hero = Hero(
    name = name,
    showImage = true,
    stickyNavTarget = true,
    disableParallax = false
  )
  
  def forSection(name: String, withImage: Boolean = false): Hero = Hero(
    name = name,
    showImage = withImage,
    disableParallax = true
  )
  
  def forFullBleed(name: String): Hero = Hero(
    name = name,
    showImage = true,
    noContent = true,
    disableParallax = false
  )
}
