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

import com.pastestack.assetgraph.Jam
import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}

/**
 * Factory for creating asset include ViewModels and jams.
 *
 * JAM factories carry module names only — no versions, timestamps or composed
 * URLs. Rendering them is the per-render paste-assetgraph page object's job.
 */
object IncludesFactory {

  case class AssetConfig(debugMode: Boolean = false)

  // Standard paste modules
  val PASTE_CORE = Seq("paste", "paste.dom", "paste.event", "paste.util")
  val PASTE_UI = Seq("paste.ui.heroscroll", "paste.ui.stickynav", "paste.ui.smoothscroll")
  val PASTE_FORMS = Seq("paste.ui.autogrow")

  /**
   * Jam for paste core + UI modules.
   */
  def forPasteJam(additionalModules: Seq[String] = Seq.empty): Jam =
    Jam(js = PASTE_CORE ++ PASTE_UI ++ additionalModules)

  /**
   * Jam for forms module (autogrow) on top of core.
   */
  def forForms(): Jam =
    Jam(js = PASTE_CORE ++ PASTE_FORMS)

  /**
   * Jam for the given CSS modules.
   */
  def forCssJam(modules: Seq[String]): Jam =
    Jam(css = modules)

  /**
   * Create JS include for direct file paths (non-JAM).
   */
  def forDirectJS(paths: Seq[String], debugMode: Boolean = false): JSInclude = {
    JSInclude(
      debugMode = debugMode,
      pasteIncludes = paths
    )
  }

  /**
   * Create CSS include for direct file paths.
   */
  def forDirectCSS(paths: Seq[String], debugMode: Boolean = false): CSSInclude = {
    CSSInclude(
      debugMode = debugMode,
      pasteIncludes = paths
    )
  }
}
