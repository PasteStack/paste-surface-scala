package com.pastestack.surface.view_models.includes

import com.pastestack.assetgraph.{AssetType, Jam, PastePage}
import play.twirl.api.Html

/**
 * The only JAM logic in the surface: record the page's jam event on the
 * per-render paste-assetgraph page and emit that page's dump for the asset
 * type, unescaped. The surface composes no URL, timestamp, version tag or
 * dependency list — the binary owns all of it. The page comes from the
 * caller's app-owned `Paste`; a non-empty jam without one fails loudly
 * rather than silently emitting no tag.
 */
object JamIncludes {

  /**
   * Record the layout's jam event on the page. An empty jam records nothing;
   * a non-empty one with no page throws rather than dropping the modules.
   * Recording only — the markup comes from `dump`.
   */
  def record(jam: Jam, page: Option[PastePage]): Html =
    if (jam.css.isEmpty && jam.js.isEmpty) Html("")
    else {
      val p = requirePage(page)
      p.jam(
        css = jam.css,
        js = jam.js,
        inline = jam.inline,
        sync = jam.sync,
        media = jam.media
      )
      Html("")
    }

  /** Emit the page's dump for the asset type; no page means no markup. */
  def dump(assetType: AssetType, page: Option[PastePage]): Html =
    page.fold(Html(""))(p => Html(p.dump(assetType)))

  /**
   * Resolve a direct-mode path: the source path in debug, otherwise the
   * `.min.` sibling. The extension is matched against the base only — a
   * query or fragment rides along untouched — and a path that is already
   * minified or ends in another extension is emitted as given rather than
   * rewritten into `foo.min.min.js`.
   */
  def directPath(path: String, extension: String, debug: Boolean): String =
    if (debug) path
    else {
      val boundary = path.indexWhere(c => c == '?' || c == '#')
      val (base, suffix) = if (boundary < 0) (path, "") else path.splitAt(boundary)
      if (base.endsWith(s".min.$extension") || !base.endsWith(s".$extension")) path
      else base.stripSuffix(s".$extension") + s".min.$extension" + suffix
    }

  private def requirePage(page: Option[PastePage]): PastePage =
    page.getOrElse(
      throw new IllegalArgumentException(
        "a non-empty jam has no paste-assetgraph page: pass the app's " +
          "per-render PastePage"
      )
    )
}
