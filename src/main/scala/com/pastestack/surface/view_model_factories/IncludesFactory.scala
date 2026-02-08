package com.pastestack.surface.view_model_factories

import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}

/**
 * Factory for creating asset include ViewModels.
 * 
 * Aggregates multiple modules into cacheable JAM URLs.
 */
object IncludesFactory {
  
  case class AssetConfig(
    version: String,
    checksum: String,
    debugMode: Boolean = false
  )
  
  // Standard paste modules
  val PASTE_CORE = Seq("paste", "paste.dom", "paste.event", "paste.util")
  val PASTE_UI = Seq("paste.ui.heroscroll", "paste.ui.stickynav", "paste.ui.smoothscroll")
  val PASTE_FORMS = Seq("paste.ui.autogrow", "paste.ui.throttle")
  
  /**
   * Create JS include for paste core + UI modules via JAM.
   */
  def forPasteJam(config: AssetConfig, additionalModules: Seq[String] = Seq.empty): JSInclude = {
    JSInclude(
      debugMode = config.debugMode,
      jamModules = PASTE_CORE ++ PASTE_UI ++ additionalModules,
      jamVersion = config.version,
      jamChecksum = config.checksum
    )
  }
  
  /**
   * Create JS include for forms (autogrow, validation).
   */
  def forForms(config: AssetConfig): JSInclude = {
    JSInclude(
      debugMode = config.debugMode,
      jamModules = PASTE_CORE ++ PASTE_FORMS,
      jamVersion = config.version,
      jamChecksum = config.checksum
    )
  }
  
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
