package com.pastestack.surface

import com.pastestack.surface.view_model_factories.IncludesFactory
import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}

/**
 * The JAM path shape is the JAM server's contract: one version segment, then the
 * `~`-separated module list. A second segment would be read as part of the first
 * module name and that module would be dropped from the response, so these checks
 * pin the shape rather than only the module set.
 */
object IncludesRenderingChecks {
  private def renderJs(include: JSInclude): String =
    paste.surface.includes.html.js(include).body

  private def src(html: String): String =
    """src="([^"]*)"""".r.findFirstMatchIn(html).map(_.group(1)).getOrElse(
      sys.error(s"no src attribute rendered:\n$html")
    )

  def main(args: Array[String]): Unit = {
    val bare = JSInclude(
      jamModules = Seq("paste.event", "paste.dom"),
      jamVersion = "2025.02.02"
    )
    assert(bare.useJam)
    assert(bare.jamUrl == "/paste/2025.02.02/paste.dom~paste.event.min.js", bare.jamUrl)
    assert(src(renderJs(bare)) == bare.jamUrl)

    val versioned = JSInclude(
      jamModules = Seq("paste.ui.heroscroll", "paste.dom", "paste.event"),
      jamVersion = "1770465409",
      jamModuleVersions = Map("paste.dom" -> "1.0", "paste.ui.heroscroll" -> "3.0")
    )
    val versionedUrl = "/paste/1770465409/paste.dom+v1.0~paste.event~paste.ui.heroscroll+v3.0.min.js"
    assert(versioned.jamUrl == versionedUrl, versioned.jamUrl)
    // One segment before the module list: /paste/{version}/{modules}
    assert(versioned.jamUrl.count(_ == '/') == 3, versioned.jamUrl)

    val debug = versioned.copy(debugMode = true)
    assert(debug.jamUrl.endsWith(".js") && !debug.jamUrl.endsWith(".min.js"), debug.jamUrl)

    val factory = IncludesFactory.forPasteJam(
      IncludesFactory.AssetConfig(version = "2025.02.02"),
      additionalModules = Seq("site.main"),
      moduleVersions = Map("site.main" -> "2.0")
    )
    assert(factory.jamUrl.startsWith("/paste/2025.02.02/"), factory.jamUrl)
    assert(factory.jamUrl.contains("site.main+v2.0"), factory.jamUrl)
    assert(factory.jamUrl.count(_ == '/') == 3, factory.jamUrl)
    assert(factory.jamModules == factory.jamModules.distinct, "no duplicate modules")

    // Direct mode still renders one tag per path, minified unless debugging.
    val direct = IncludesFactory.forDirectJS(Seq("/js/app.js"))
    assert(!direct.useJam)
    assert(src(renderJs(direct)) == "/js/app.min.js", renderJs(direct))
    assert(src(renderJs(IncludesFactory.forDirectJS(Seq("/js/app.js"), debugMode = true))) == "/js/app.js")

    val css = CSSInclude(pasteIncludes = Seq("/css/site.css"))
    assert(paste.surface.includes.html.css(css).body.contains("/css/site.min.css"))
  }
}
