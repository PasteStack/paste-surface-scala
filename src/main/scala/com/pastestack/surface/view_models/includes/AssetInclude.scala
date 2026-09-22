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
  // JAM mode: module names resolved by the JAM server from one cacheable path
  jamModules: Seq[String] = Seq.empty,
  // Immutable cache segment for the group: the asset build version, or the
  // manifest's group timestamp when the caller reads one.
  jamVersion: String = "",
  // Per-module versions from the asset manifest, when the caller has one. The
  // server strips them; they exist so one changed module invalidates the path.
  jamModuleVersions: Map[String, String] = Map.empty
) extends AssetInclude {
  def useJam: Boolean = jamModules.nonEmpty

  /**
   * Cacheable JAM path: /paste/{version}/{module+vN.0~...}{.min}.js
   *
   * The server reads everything after the version segment as the module list, so
   * this path carries exactly one version segment. Modules are separated by `~`
   * and sorted, so the same set always yields the same cacheable path.
   */
  def jamUrl: String = {
    val modules = jamModules.sorted
      .map(module => jamModuleVersions.get(module).fold(module)(version => s"$module+v$version"))
      .mkString("~")
    val suffix = if (debugMode) ".js" else ".min.js"
    s"/paste/$jamVersion/$modules$suffix"
  }
}
