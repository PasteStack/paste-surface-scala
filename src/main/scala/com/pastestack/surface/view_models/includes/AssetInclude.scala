package com.pastestack.surface.view_models.includes

trait AssetInclude {
  def debugMode: Boolean
  def pasteIncludes: Seq[String]
}

case class CSSInclude(
  debugMode: Boolean = false,
  pasteIncludes: Seq[String] = Seq.empty
) extends AssetInclude

case class JSInclude(
  debugMode: Boolean = false,
  pasteIncludes: Seq[String] = Seq.empty,
  async: Boolean = false,
  defer: Boolean = true,
  // JAM mode: specify module names, resolved via cacheable path
  // URL pattern: /paste/{version}/{checksum}/{modules}.js
  jamModules: Seq[String] = Seq.empty,
  jamVersion: String = "",
  jamChecksum: String = ""
) extends AssetInclude {
  def useJam: Boolean = jamModules.nonEmpty
  
  // Cacheable URL: /paste/2025.02.02/abc123/paste.dom,paste.ui.heroscroll.js
  def jamUrl: String = {
    val modules = jamModules.sorted.mkString(",")
    val suffix = if (debugMode) ".js" else ".min.js"
    s"/paste/$jamVersion/$jamChecksum/$modules$suffix"
  }
}
