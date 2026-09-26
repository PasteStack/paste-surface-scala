package com.pastestack.surface.view_models.includes

trait AssetInclude {
  // Direct mode only: source files instead of minified. JAM markup is
  // decided by the paste-assetgraph build, not by the include.
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
  defer: Boolean = true
) extends AssetInclude
