package com.pastestack.surface.view_model_factories

import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}

/**
 * Factory for creating asset include ViewModels.
 *
 * JAM factories carry module names only — no versions, timestamps or composed
 * URLs. Rendering them is the per-render paste-assetgraph page object's job.
 */
object IncludesFactory {

  case class AssetConfig(debugMode: Boolean = false)

  // Standard paste modules
  val PASTE_CORE = Seq("paste", "paste.dom", "paste.event", "paste.util")
  val PASTE_UI = Seq("paste.ui.heroscroll", "paste.ui.stickynav", "paste.ui.smoothscroll")
  val PASTE_FORMS = Seq("paste.ui.autogrow", "paste.ui.utilities.throttle")

  /**
   * Create JS include for paste core + UI modules via JAM.
   */
  def forPasteJam(additionalModules: Seq[String] = Seq.empty): JSInclude =
    JSInclude(jamModules = PASTE_CORE ++ PASTE_UI ++ additionalModules)

  /**
   * Create JS include for forms (autogrow, throttle) via JAM.
   */
  def forForms(): JSInclude =
    JSInclude(jamModules = PASTE_CORE ++ PASTE_FORMS)

  /**
   * Create CSS include for the given modules via JAM.
   */
  def forCssJam(modules: Seq[String]): CSSInclude =
    CSSInclude(jamModules = modules)

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
