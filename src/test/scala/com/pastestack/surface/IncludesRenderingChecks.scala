package com.pastestack.surface

import java.nio.file.{Files, Path, Paths}
import com.pastestack.assetgraph.{AssetType, Jam, Paste, PasteException, PastePage}
import com.pastestack.surface.view_model_factories.IncludesFactory
import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude, JamIncludes}

/**
 * JAM operations delegate to the per-render paste-assetgraph page: the
 * layout records one jam event on the page and emits that page's dumps
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

  private def renderJs(include: JSInclude): String =
    paste.surface.includes.html.js(include).body

  private def renderCss(include: CSSInclude): String =
    paste.surface.includes.html.css(include).body

  private def src(html: String): String =
    """src="([^"]*)"""".r.findFirstMatchIn(html).map(_.group(1)).getOrElse(
      sys.error(s"no src attribute rendered:\n$html")
    )

  def main(args: Array[String]): Unit = {
    assert(IncludesFactory.PASTE_FORMS == Seq("paste.ui.autogrow"))
    // record + dump emits exactly the page's own jam + dump markup — the
    // surface adds and removes nothing, so it composes no URL of its own.
    val jam = Jam(js = Seq("app"))
    val jsPage = pasteAssetgraph.page()
    JamIncludes.record(jam, Some(jsPage))
    val jsRendered = JamIncludes.dump(AssetType.Js, Some(jsPage)).body
    val jsExpected = {
      val page = pasteAssetgraph.page()
      page.jam(js = Seq("app"))
      page.dump(AssetType.Js)
    }
    assert(jsRendered == jsExpected, s"dump diverged from the page's own:\n$jsRendered\n$jsExpected")
    assert(jsRendered.contains("<script"), jsRendered)

    // A css jam carries its media through the page dump.
    val cssPage = pasteAssetgraph.page()
    JamIncludes.record(Jam(css = Seq("site.theme"), media = Some("print")), Some(cssPage))
    val cssRendered = JamIncludes.dump(AssetType.Css, Some(cssPage)).body
    val cssExpected = {
      val page = pasteAssetgraph.page()
      page.jam(css = Seq("site.theme"), media = Some("print"))
      page.dump(AssetType.Css)
    }
    assert(cssRendered == cssExpected, s"dump diverged from the page's own:\n$cssRendered\n$cssExpected")
    assert(cssRendered.contains("""media="print""""), cssRendered)

    // CSS then JS through the SAME page: each dump emits only its own type,
    // and a second dump does not name an already-delivered module again.
    val page = pasteAssetgraph.page()
    JamIncludes.record(Jam(css = Seq("site.theme"), js = Seq("app")), Some(page))
    val head = JamIncludes.dump(AssetType.Css, Some(page)).body
    assert(head.contains("site.theme"), head)
    val tail = JamIncludes.dump(AssetType.Js, Some(page)).body
    assert(!tail.contains("site.theme"), tail)
    assert(tail.contains("<script"), tail)
    val again = JamIncludes.dump(AssetType.Js, Some(page)).body
    assert(!again.contains("app+v1.0"), again)

    // A non-empty jam without a page fails loudly rather than emitting no
    // tag; an empty jam records nothing and needs none.
    val thrown = try {
      JamIncludes.record(jam, None)
      sys.error("expected IllegalArgumentException")
    } catch {
      case e: IllegalArgumentException => e.getMessage
    }
    assert(thrown.contains("paste-assetgraph") && thrown.contains("PastePage"), thrown)
    assert(JamIncludes.record(Jam(), None).body.isEmpty)
    // A dump with no page emits nothing.
    assert(JamIncludes.dump(AssetType.Css, None).body.isEmpty)
    assert(JamIncludes.dump(AssetType.Js, None).body.isEmpty)

    // A combined dump on the page delivers both types at once — css markup
    // first — and finish seals it.
    val combined = pasteAssetgraph.page()
    combined.jam(css = Seq("site.theme"), js = Seq("app"))
    val combinedHtml = combined.dump()
    assert(combinedHtml.contains("site.theme"), combinedHtml)
    assert(combinedHtml.indexOf("site.theme") < combinedHtml.indexOf("app+v1.0"), combinedHtml)
    assert(combined.finish() == "")
    assert(combined.isFinished)

    // Native fragment links require no navigation module.
    assert(IncludesFactory.forPasteJam().js.contains("paste.ui.smoothscroll"))

    // Factory jams carry names only.
    assert(
      IncludesFactory.forForms().js ==
        IncludesFactory.PASTE_CORE ++ IncludesFactory.PASTE_FORMS
    )
    assert(
      IncludesFactory.forPasteJam(Seq("site.main")).js ==
        IncludesFactory.PASTE_CORE ++ IncludesFactory.PASTE_UI ++ Seq("site.main")
    )
    assert(
      IncludesFactory.forCssJam(Seq("site.theme")).css == Seq("site.theme")
    )

    // jambase records the jam, dumps both types when given the page object —
    // and seals it: the layout's closing body calls finish, which the
    // complete page passes.
    val layoutPage = pasteAssetgraph.page()
    val layout = paste.surface.layouts.html.jambase(
      PageMeta(siteName = "Site", title = "Home", description = ""),
      page = Some(layoutPage),
      jam = Jam(css = Seq("site.theme"), js = Seq("app"))
    )()()(play.twirl.api.Html("<p>hi</p>"))()().body
    assert(layout.contains("site.theme"), layout)
    assert(layout.contains("<script"), layout)
    assert(layoutPage.isFinished, "jambase must finish the page it was given")

    // A jam whose type never reaches a dump fails finish naming the module
    // — the check runs through the same delegation the layout uses: record
    // the css jam, dump only js, and the page refuses to seal.
    val late = pasteAssetgraph.page()
    JamIncludes.record(Jam(css = Seq("site.theme"), js = Seq("app")), Some(late))
    JamIncludes.dump(AssetType.Js, Some(late))
    val missing = try {
      late.finish()
      sys.error("expected PasteException")
    } catch {
      case e: PasteException => e.getMessage
    }
    assert(missing.contains("site.theme"), missing)
    assert(!late.isFinished, "a failed finish must not seal the page")
    // The page stays open: deliver the missing dump, then finish.
    JamIncludes.dump(AssetType.Css, Some(late))
    assert(late.finish() == "")
    assert(late.isFinished)

    // Direct mode still renders one tag per path, minified unless debugging.
    val direct = IncludesFactory.forDirectJS(Seq("/js/app.js"))
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
