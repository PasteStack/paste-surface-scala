package com.pastestack.surface

import com.pastestack.surface.view_model_factories.AdaptiveImageSequenceManifest
import com.pastestack.surface.view_model_factories.AdaptiveImageSequenceManifest.InvalidManifest

// The adaptive manifest adapter's contract at the conversion boundary:
// the source document validates by name, tiers serialize narrowest-
// first with each wider tier an upgrade rung, and every emitted src is
// whatever the caller's resolver returned — absolute, protocol-
// relative, and data URIs pass through untouched when it says so.
// Resolution policy lives with the caller; the test resolver below passes
// srcs carrying a scheme or a // authority verbatim and joins the rest
// onto a base.
object AdaptiveImageSequenceManifestChecks {

  private def resource(name: String): String = {
    val stream = Option(getClass.getResourceAsStream(s"/sequence-manifests/$name/manifest.json"))
      .getOrElse(throw new IllegalStateException(s"fixture missing: $name"))
    try new String(stream.readAllBytes(), "UTF-8") finally stream.close()
  }

  // A representative caller resolver: srcs carrying their own
  // resolution context pass verbatim; anything else joins the base on
  // exactly one boundary slash. The adapter never invents this.
  private val ownAuthority = "^[a-zA-Z][a-zA-Z0-9+.-]*:|^//".r
  private def resolve(base: String)(src: String): String =
    if (ownAuthority.findFirstIn(src).isDefined) src
    else s"${base.stripSuffix("/")}/${src.stripPrefix("/")}"

  private def parse(name: String): AdaptiveImageSequenceManifest =
    AdaptiveImageSequenceManifest.parseJson(resource(name))

  private def rejected(json: => String, fragment: String, what: String): Unit = {
    val failure = try {
      AdaptiveImageSequenceManifest.parseJson(json)
      throw new IllegalStateException("parse unexpectedly succeeded")
    } catch {
      case e: InvalidManifest => e
    }
    assert(failure.getMessage.contains(fragment),
      s"$what must fail naming '$fragment' — was: ${failure.getMessage}")
  }

  private def checkTwoBases(): Unit = {
    val manifest = parse("adaptive")
    val first = manifest.toCatalog(resolve("https://cdn-a.example.com/library"))
    val second = manifest.toCatalog(resolve("/static/sequences/fixture"))
    assert(first.frames.head.src == "https://cdn-a.example.com/library/480/frame_0001.webp")
    assert(second.frames.head.src == "/static/sequences/fixture/480/frame_0001.webp")
    assert(first.frames.head.upgrades.head.src == "https://cdn-a.example.com/library/960/frame_0001.webp")
    assert(second.frames.head.upgrades.head.src == "/static/sequences/fixture/960/frame_0001.webp")
    // The adapter holds no state: a second conversion never inherits
    // the first call's resolution context.
    assert(second.frames.forall(frame =>
      !frame.src.contains("cdn-a.example.com") &&
        frame.upgrades.forall(!_.src.contains("cdn-a.example.com"))))
  }

  private def checkOwnAuthorityPassthrough(): Unit = {
    // Whatever the resolver returns is what the catalog emits — the
    // adapter neither prefixes nor rewrites. A verbatim resolver must
    // let absolute, protocol-relative and data: srcs through intact.
    val catalog = parse("adaptive").toCatalog(identity)
    assert(catalog.frames(1).src == "https://cdn.example.com/shared/frame_0002.webp")
    assert(catalog.frames(1).upgrades.head.src == "//cdn.example.com/shared/frame_0002-960.webp")
    assert(catalog.frames(3).src.startsWith("data:image/webp;base64,"))
    // A site-base resolver receives the same raw srcs the document
    // carries — it decides what joins the base, not the adapter.
    val resolved = parse("adaptive").toCatalog(resolve("https://site.example.com/assets"))
    assert(resolved.frames(1).src == "https://cdn.example.com/shared/frame_0002.webp")
    assert(resolved.frames(1).upgrades.head.src == "//cdn.example.com/shared/frame_0002-960.webp")
    assert(resolved.frames.forall(frame =>
      !frame.src.contains("site.example.com/https") &&
        !frame.src.contains("site.example.com/data") &&
        !frame.src.contains("site.example.com//cdn")))
    // Root-relative sources reach the resolver unmodified.
    assert(resolved.frames(2).upgrades.head.src == "https://site.example.com/assets/960/frame_0003.webp")
  }

  private def checkBaseEdgeCases(): Unit = {
    val manifest = parse("adaptive")
    val plain = manifest.toCatalog(resolve("https://cdn-c.example.com/x"))
    val slashed = manifest.toCatalog(resolve("https://cdn-c.example.com/x/"))
    assert(plain.frames.map(_.src) == slashed.frames.map(_.src))
    assert(plain.frames.map(_.upgrades.map(_.src)) == slashed.frames.map(_.upgrades.map(_.src)))
    val rooted = manifest.toCatalog(resolve(""))
    assert(rooted.frames.head.src == "/480/frame_0001.webp")
    assert(rooted.frames.head.upgrades.head.src == "/960/frame_0001.webp")
  }

  private def checkTierOrdering(): Unit = {
    // An out-of-order document produces the same ascending ladder —
    // sorting happens once at parse, not per call.
    val reversed = """{"schemaVersion":1,"count":2,"sizes":[
        {"width":960,"height":540,"frames":[{"src":"960/a.webp"},{"src":"960/b.webp"}]},
        {"width":480,"height":270,"frames":[{"src":"480/a.webp"},{"src":"480/b.webp"}]}
      ]}"""
    val catalog = AdaptiveImageSequenceManifest.parseJson(reversed).toCatalog(identity)
    assert(catalog.frames.head.src == "480/a.webp")
    assert(catalog.frames.head.upgrades.head.src == "960/a.webp")
    assert(catalog.frames.head.upgrades.head.width == 960)
  }

  private def checkNegativeFixtures(): Unit = {
    rejected(resource("no-sizes"), "sizes", "a manifest with no sizes array")
    rejected(resource("empty-sizes"), "sizes", "an empty sizes array")
    rejected(resource("missing-frames"), "frames", "a tier without frames")
    rejected(resource("frames-not-array"), "frames", "a non-array frames field")
    rejected(resource("missing-width"), "width", "a tier without a dimension")
    rejected(resource("wrong-type-width"), "width", "a tier with a non-numeric dimension")
    rejected(resource("missing-src"), "src", "a frame without src")
    rejected(resource("uneven-tiers"), "frame count", "tiers with different frame counts")
    rejected(resource("absent-count"), "count", "a manifest without a declared count")
    rejected(resource("invalid-count"), "count", "a non-integer declared count")
    rejected(resource("mismatched-count"), "frame count", "a declared count disagreeing with the tiers")
    rejected(resource("unsupported-version"), "schemaVersion", "an unsupported format version")
    rejected(resource("duplicate-tiers"), "duplicate tier width", "duplicate tier widths")
  }

  private def checkDocumentRejects(): Unit = {
    Seq(
      "null" -> "a null document",
      "[]" -> "an array document",
      "\"x\"" -> "a string document",
      "42" -> "a number document",
      "{" -> "a truncated document",
      "not json {" -> "a malformed document"
    ).foreach { case (json, what) =>
      val failure = try {
        AdaptiveImageSequenceManifest.parseJson(json)
        throw new IllegalStateException("parse unexpectedly succeeded")
      } catch {
        case e: InvalidManifest => e
      }
      assert(failure.getMessage.nonEmpty, s"$what must fail with a named message")
    }
    // An otherwise valid first root must not conceal a second document.
    // Duplicate JSON properties must not silently overwrite one another.
    val valid = """{"schemaVersion":1,"count":1,"sizes":[
      {"width":480,"height":270,"frames":[{"src":"480/a.webp"}]}
    ]}"""
    Seq(
      (valid + " true") -> "a trailing JSON value",
      (valid + " {}") -> "a second JSON document",
      valid.replace("\"count\":1", "\"count\":1,\"count\":1") ->
        "a duplicate document count",
      valid.replace("\"width\":480", "\"width\":480,\"width\":960") ->
        "a duplicate tier dimension"
    ).foreach { case (json, what) =>
      val rejected = try {
        AdaptiveImageSequenceManifest.parseJson(json)
        false
      } catch {
        case _: InvalidManifest => true
      }
      assert(rejected, s"$what must fail as InvalidManifest")
    }
    rejected(null, "absent or blank", "an absent document")
    rejected("   \n", "absent or blank", "a blank document")
  }

  private def checkResolvedAliasConflict(): Unit = {
    // Two raw srcs that differ lexically may still name one resource —
    // `//cdn.example.com/...` resolves against the document scheme. The
    // adapter passes them through verbatim; when a caller's resolver
    // collapses them onto one URL at two intrinsic sizes the typed
    // frame refuses, which is what the Elements module would reject in
    // the browser.
    val manifest = parse("conflicting-alias")
    val httpsResolve = (src: String) =>
      if (src.startsWith("//")) s"https:$src" else src
    val rejected = try {
      manifest.toCatalog(httpsResolve)
      false
    } catch {
      case _: IllegalArgumentException => true
    }
    assert(rejected, "a base and upgrade resolving to one resource at two intrinsic sizes must fail")
  }

  // A fixture-level guard the raw-string assertions cannot give: resolve
  // every declared source against a representative HTTPS document URI and
  // group by resolved URL. One resource may claim exactly one intrinsic
  // size; a fixture edit that reintroduces an alias collision fails here
  // even when the lexical srcs still differ. java.net.URI only
  // approximates browser URL parsing — it keeps host case and default
  // ports and rejects spaces — so fixture srcs stay plain and lowercase.
  private def normalizedConflicts(name: String): Map[String, Set[(Int, Int)]] = {
    val documentBase = new java.net.URI("https://docs.example.com/page/index.html")
    val catalog = parse(name).toCatalog(identity)
    catalog.frames
      .flatMap(frame =>
        (frame.src, (frame.width, frame.height)) +:
          frame.sortedUpgrades.map(u => (u.src, (u.width, u.height))))
      .map { case (src, size) => documentBase.resolve(src).toString -> size }
      .groupMap(_._1)(_._2)
      .map { case (url, sizes) => url -> sizes.toSet }
      .filter { case (_, sizes) => sizes.size > 1 }
  }

  private def checkNormalizedUniqueness(): Unit = {
    assert(
      normalizedConflicts("adaptive").isEmpty,
      "the canonical fixture must hold no post-resolution dimension conflicts"
    )
    val conflicts = normalizedConflicts("conflicting-alias")
    assert(
      conflicts == Map("https://cdn.example.com/shared/frame_0002.webp" -> Set((480, 270), (960, 540))),
      s"the conflicting fixture must surface its declared collision — was: $conflicts"
    )
  }

  private def checkTypedBoundaries(): Unit = {
    // Tier geometry is the typed frame's rule set, applied once at parse:
    // an aspect conflict or a src repeated across tiers is a manifest
    // defect, named InvalidManifest, never a render-time failure.
    rejected("""{"schemaVersion":1,"count":1,"sizes":[
        {"width":480,"height":270,"frames":[{"src":"480/a.webp"}]},
        {"width":960,"height":700,"frames":[{"src":"960/a.webp"}]}
      ]}""", "valid ladder", "a tier aspect conflict")
    rejected("""{"schemaVersion":1,"count":1,"sizes":[
        {"width":854,"height":480,"frames":[{"src":"854/a.webp"}]},
        {"width":856,"height":480,"frames":[{"src":"856/a.webp"}]}
      ]}""", "valid ladder", "a wider tier that is not taller")
    rejected("""{"schemaVersion":1,"count":1,"sizes":[
        {"width":480,"height":270,"frames":[{"src":"a.webp"}]},
        {"width":960,"height":540,"frames":[{"src":"a.webp"}]}
      ]}""", "repeats a frame src", "a src repeated across tiers")
    // The repeat need not sit at the same frame index.
    rejected("""{"schemaVersion":1,"count":2,"sizes":[
        {"width":480,"height":270,"frames":[{"src":"a.webp"},{"src":"b.webp"}]},
        {"width":960,"height":540,"frames":[{"src":"b.webp"},{"src":"c.webp"}]}
      ]}""", "repeats a frame src", "a src repeated across tiers at another index")
    // Spellings of one URL are one src: the browser removes dot segments,
    // "%2e" spellings and a ".." at the root included, and folds an
    // http(s) scheme and host and a default port, whatever base it
    // resolves against.
    Seq(
      ("./480/a.webp", "480/a.webp", "a ./ spelling"),
      ("x/../a.webp", "a.webp", "a ../ spelling inside the path"),
      ("HTTPS://CDN.example.com/a.webp", "https://cdn.example.com/a.webp", "a scheme and host spelled in capitals"),
      ("%2e/a.webp", "a.webp", "a %2e spelling"),
      ("480/%2E%2e/a.webp", "a.webp", "a %2E%2e spelling"),
      ("/../a.webp", "/a.webp", "a .. at the root"),
      ("https://cdn.example.com:443/a.webp", "https://cdn.example.com/a.webp", "a default port"),
      ("x//../a.webp", "x/a.webp", "a .. that removes an empty segment")
    ).foreach { case (narrow, wide, what) =>
      rejected(s"""{"schemaVersion":1,"count":1,"sizes":[
          {"width":480,"height":270,"frames":[{"src":"$narrow"}]},
          {"width":960,"height":540,"frames":[{"src":"$wide"}]}
        ]}""", "repeats a frame src", s"$what repeated across tiers")
    }
    // A leading ".." climbs a base the manifest does not know, a path
    // keeps its case, an empty segment is a segment a ".." removes
    // ("x//../a.webp" is "x/a.webp"), and a port other than the scheme's
    // own is part of the URL: all stay distinct srcs, as does a same-tier
    // repeat.
    Seq(
      ("../a.webp", "a.webp"),
      ("https://cdn.example.com/A.webp", "https://cdn.example.com/a.webp"),
      ("x//../a.webp", "a.webp"),
      ("http://cdn.example.com:443/a.webp", "http://cdn.example.com/a.webp")
    ).foreach { case (narrow, wide) =>
      AdaptiveImageSequenceManifest.parseJson(s"""{"schemaVersion":1,"count":1,"sizes":[
          {"width":480,"height":270,"frames":[{"src":"$narrow"}]},
          {"width":960,"height":540,"frames":[{"src":"$wide"}]}
        ]}""")
    }
    AdaptiveImageSequenceManifest.parseJson("""{"schemaVersion":1,"count":2,"sizes":[
        {"width":480,"height":270,"frames":[{"src":"./a.webp"},{"src":"a.webp"}]},
        {"width":960,"height":540,"frames":[{"src":"960/a.webp"},{"src":"960/a.webp"}]}
      ]}""")
    // A src is the Element's: blank after JavaScript's wider trim, or
    // carrying a C0 control, is refused at parse, not at render.
    Seq(
      "\\u00a0" -> "a no-break-space src",
      "\\ufeff" -> "a byte-order-mark src",
      "a\\u0007b.webp" -> "a control-character src"
    ).foreach { case (src, what) =>
      rejected(s"""{"schemaVersion":1,"count":1,"sizes":[
          {"width":480,"height":270,"frames":[{"src":"$src"}]}
        ]}""", "non-blank string", what)
    }
    // A null resolver is caller misuse, not a manifest defect — the
    // method refuses it by name rather than dereference it on the first
    // src.
    val manifest = parse("adaptive")
    val nullResolver = try {
      manifest.toCatalog(null)
      false
    } catch {
      case e: IllegalArgumentException =>
        e.getMessage.contains("resolveSource")
    }
    assert(nullResolver, "a null resolver must fail naming resolveSource")
  }

  private def checkNoPoisonedState(): Unit = {
    // A rejected parse raises before any value exists; a later
    // successful parse is unaffected.
    try AdaptiveImageSequenceManifest.parseJson(resource("no-sizes")) catch {
      case _: InvalidManifest => ()
    }
    val catalog = parse("adaptive").toCatalog(identity)
    assert(catalog.frames.length == 4 && catalog.frames.forall(_.upgrades.length == 1))
  }

  def main(args: Array[String]): Unit = {
    checkTwoBases()
    checkOwnAuthorityPassthrough()
    checkBaseEdgeCases()
    checkTierOrdering()
    checkNegativeFixtures()
    checkDocumentRejects()
    checkResolvedAliasConflict()
    checkNormalizedUniqueness()
    checkTypedBoundaries()
    checkNoPoisonedState()
  }
}
