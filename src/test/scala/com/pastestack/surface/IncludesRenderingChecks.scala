package com.pastestack.surface

import java.nio.file.{Files, Path, Paths}
import com.pastestack.assetgraph.{Paste, PasteException, PastePage}
import com.pastestack.surface.view_model_factories.IncludesFactory
import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}

/**
 * JAM-mode includes delegate to the per-render paste-assetgraph page: the
 * include records its jam event on the page and emits that page's dump
 * verbatim — the surface composes no URL, timestamp, version tag or
 * dependency list. These checks run the real native library against the
 * fixture under
 * `src/test/resources/paste-fixture` (a `paste` runtime module, a `site.theme`
 * CSS module, and an `app` JS module that depends on `paste`); the library
 * resolves from the runtime artifact unless PASTE_ASSETGRAPH_LIB is
 * explicitly set.
 */
object IncludesRenderingChecks {
  // The forked runMain runs with the project base as its working directory.
  private lazy val fixtureRoot: Path = {
    val dir = Paths.get("src/test/resources/paste-fixture")
    require(Files.isDirectory(dir), s"fixture directory not found: ${dir.toAbsolutePath}")
    dir
  }

  private lazy val pasteAssetgraph: Paste = Paste(Paste.Settings(
    manifest = fixtureRoot.resolve("manifest.json"),
    outputBase = fixtureRoot.resolve("public"),
    // No automatic inlining: the dumps produce external tags.
    inlineThreshold = 0
  ))

  private def renderJs(include: JSInclude, page: Option[PastePage] = None): String =
    paste.surface.includes.html.js(include, page).body

  private def renderCss(include: CSSInclude, page: Option[PastePage] = None): String =
    paste.surface.includes.html.css(include, page).body

  private def src(html: String): String =
    """src="([^"]*)"""".r.findFirstMatchIn(html).map(_.group(1)).getOrElse(
      sys.error(s"no src attribute rendered:\n$html")
    )

  def main(args: Array[String]): Unit = {
    // JAM mode: the include emits exactly the page's dump — the surface adds
    // and removes nothing, so the markup carries no surface-composed URL.
    val jsInclude = JSInclude(jamModules = Seq("app"))
    val jsRendered = renderJs(jsInclude, Some(pasteAssetgraph.page()))
    val jsExpected = {
      val page = pasteAssetgraph.page()
      page.jam(js = Seq("app"))
      page.dumpJs()
    }
    // Template framing adds incidental whitespace; the markup is the dump.
    assert(jsRendered.trim == jsExpected.trim, s"include output diverged from the page dump:\n$jsRendered\n$jsExpected")
    assert(jsRendered.contains("<script"), jsRendered)

    // CSS JAM mode with media: the page dump verbatim, media carried through.
    val cssInclude = CSSInclude(jamModules = Seq("site.theme"), jamMedia = Some("print"))
    val cssRendered = renderCss(cssInclude, Some(pasteAssetgraph.page()))
    val cssExpected = {
      val page = pasteAssetgraph.page()
      page.jam(css = Seq("site.theme"), media = Some("print"))
      page.dumpCss()
    }
    assert(cssRendered.trim == cssExpected.trim, s"include output diverged from the page dump:\n$cssRendered\n$cssExpected")
    assert(cssRendered.contains("""media="print""""), cssRendered)

    // CSS then JS through the SAME page: each dump emits only its own type,
    // and a second JS include on the page does not name an already-delivered
    // module again.
    val page = pasteAssetgraph.page()
    val head = renderCss(CSSInclude(jamModules = Seq("site.theme")), Some(page))
    assert(head.contains("site.theme"), head)
    val tail = renderJs(JSInclude(jamModules = Seq("app")), Some(page))
    assert(!tail.contains("site.theme"), tail)
    assert(tail.contains("<script"), tail)
    val again = renderJs(JSInclude(jamModules = Seq("app")), Some(page))
    assert(!again.contains("app+v1.0"), again)

    // JAM mode without a page fails loudly rather than emitting no tag.
    val thrown = try {
      renderJs(jsInclude)
      sys.error("expected IllegalArgumentException")
    } catch {
      case e: IllegalArgumentException => e.getMessage
    }
    assert(thrown.contains("paste-assetgraph") && thrown.contains("PastePage"), thrown)

    // Factory module lists carry names only.
    assert(
      IncludesFactory.forForms().jamModules ==
        IncludesFactory.PASTE_CORE ++ IncludesFactory.PASTE_FORMS
    )
    assert(
      IncludesFactory.forPasteJam(Seq("site.main")).jamModules ==
        IncludesFactory.PASTE_CORE ++ IncludesFactory.PASTE_UI ++ Seq("site.main")
    )
    assert(
      IncludesFactory.forCssJam(Seq("site.theme")).jamModules == Seq("site.theme")
    )

    // jambase renders both dumps when given the page object — and seals it:
    // the layout's closing body calls finish, which the complete page
    // passes.
    val layoutPage = pasteAssetgraph.page()
    val layout = paste.surface.layouts.html.jambase(
      PageMeta(siteName = "Site", title = "Home", description = ""),
      CSSInclude(jamModules = Seq("site.theme")),
      JSInclude(jamModules = Seq("app")),
      Some(layoutPage)
    )()()(play.twirl.api.Html("<p>hi</p>"))()().body
    assert(layout.contains("site.theme"), layout)
    assert(layout.contains("<script"), layout)
    assert(layoutPage.isFinished, "jambase must finish the page it was given")

    // The layout's own finish is the check: a css jam stranded without a
    // css dump — here a direct-mode include emits no dump — must fail the
    // render naming the module, not ship a page that lost it.
    val late = pasteAssetgraph.page()
    late.jam(css = Seq("site.theme"))
    val missing = try {
      paste.surface.layouts.html.jambase(
        PageMeta(siteName = "Site", title = "Home", description = ""),
        CSSInclude(pasteIncludes = Seq("/css/site.css")),
        JSInclude(jamModules = Seq("app")),
        Some(late)
      )()()(play.twirl.api.Html("<p>hi</p>"))()()
      sys.error("expected PasteException")
    } catch {
      case e: PasteException => e.getMessage
    }
    assert(missing.contains("site.theme"), missing)
    assert(!late.isFinished, "a failed finish must not seal the page")
    // The page stays open: deliver the missing dump, then finish.
    late.dumpCss()
    assert(late.finish() == "")
    assert(late.isFinished)

    // Direct mode still renders one tag per path, minified unless debugging.
    val direct = IncludesFactory.forDirectJS(Seq("/js/app.js"))
    assert(!direct.useJam)
    assert(src(renderJs(direct)) == "/js/app.min.js", renderJs(direct))
    assert(src(renderJs(IncludesFactory.forDirectJS(Seq("/js/app.js"), debugMode = true))) == "/js/app.js")

    val css = CSSInclude(pasteIncludes = Seq("/css/site.css"))
    assert(renderCss(css).contains("/css/site.min.css"))

    // An already-minified path is emitted as given — never foo.min.min.js —
    // and a query or fragment rides along after the extension rewrite.
    def directJs(path: String, debug: Boolean = false): String =
      src(renderJs(IncludesFactory.forDirectJS(Seq(path), debugMode = debug)))
    def directCss(path: String, debug: Boolean = false): String =
      """href="([^"]*)"""".r.findFirstMatchIn(renderCss(CSSInclude(
        debugMode = debug, pasteIncludes = Seq(path)
      ))).map(_.group(1)).getOrElse(sys.error("no href rendered"))

    assert(directJs("/js/app.min.js") == "/js/app.min.js")
    assert(directCss("/css/site.min.css") == "/css/site.min.css")
    assert(directJs("/js/app.js?v=2") == "/js/app.min.js?v=2")
    assert(directJs("/js/app.js#widget") == "/js/app.min.js#widget")
    assert(directCss("/css/site.css?v=2#top") == "/css/site.min.css?v=2#top")
    // Debug preserves the path exactly, query and fragment included.
    assert(directJs("/js/app.min.js", debug = true) == "/js/app.min.js")
    assert(directJs("/js/app.js?v=2", debug = true) == "/js/app.js?v=2")
    assert(directCss("/css/site.css#top", debug = true) == "/css/site.css#top")
    // A path ending in another extension is left alone.
    assert(directJs("/js/app.jsx") == "/js/app.jsx")
  }
}
