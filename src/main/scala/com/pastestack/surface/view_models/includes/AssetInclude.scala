package com.pastestack.surface.view_models.includes

trait AssetInclude {
  // Direct mode only: source files instead of minified. JAM-mode markup is
  // decided by the paste-assetgraph build, not by the include.
  def debugMode: Boolean
  def pasteIncludes: Seq[String]
  // Non-empty selects JAM mode: the include records a jam event on the
  // per-render paste-assetgraph page and emits that page's dump.
  def jamModules: Seq[String]
  def jamInline: Boolean
  final def useJam: Boolean = jamModules.nonEmpty
}

case class CSSInclude(
  debugMode: Boolean = false,
  pasteIncludes: Seq[String] = Seq.empty,
  jamModules: Seq[String] = Seq.empty,
  jamInline: Boolean = false,
  jamMedia: Option[String] = None
) extends AssetInclude

case class JSInclude(
  debugMode: Boolean = false,
  pasteIncludes: Seq[String] = Seq.empty,
  // Direct mode only: JAM loading is decided by jamSync, not async/defer.
  async: Boolean = false,
  defer: Boolean = true,
  jamModules: Seq[String] = Seq.empty,
  jamInline: Boolean = false,
  jamSync: Boolean = false
) extends AssetInclude
