package com.pastestack.surface

import com.fasterxml.jackson.databind.ObjectMapper
import play.twirl.api.Html

object ImageSequenceRenderingChecks {
  private def render(sequence: ImageSequence, mediaId: String): String =
    paste.surface.components.html.imagesequence(sequence, MediaId(mediaId)).body

  // The text of one inert JSON block, keyed on its own id and ended at the
  // first closing tag after it — never the first one in the document.
  private def jsonBlock(html: String, id: String): String = {
    val marker = s"""<script id="$id" type="application/json">"""
    val start = html.indexOf(marker)
    assert(start >= 0, s"script#$id missing:\n$html")
    val from = start + marker.length
    html.substring(from, html.indexOf("</script>", from))
  }

  private val json = new ObjectMapper()

  private def catalog(frames: ImageSequenceFrame*): ImageSequenceCatalog =
    ImageSequenceCatalog(frames.toSeq)

  private def frame(src: String, width: Int = 640, height: Int = 360, upgrades: Seq[ImageRepresentation] = Seq.empty): ImageSequenceFrame =
    ImageSequenceFrame(src, width, height, upgrades)

  private val twoFrame: ImageSequenceCatalog = catalog(
    frame("a-640.jpg", upgrades = Seq(
      ImageRepresentation("a-1280.jpg", 1280, 720),
      ImageRepresentation("a-1920.jpg", 1920, 1080)
    )),
    frame("b-640.jpg", upgrades = Seq(ImageRepresentation("b-1280.jpg", 1280, 720)))
  )

  private def checkStructure(html: String, mediaId: String, sequence: ImageSequence): Unit = {
    assert(html.contains(s"""<div id="$mediaId""""), s"scope must carry the media id:\n$html")
    assert(html.contains("paste-ui-imagesequence-scope"), "scope class missing")
    assert(html.contains("data-paste-scrollscrub"), "scope marker missing")
    assert(html.contains("""data-scrub-start="top top""""), "scrub start missing")
    assert(html.contains("""data-scrub-end="bottom top""""), "scrub end missing")
    assert(html.contains(s"""<i id="$mediaId-host""""), "host element missing")
    assert(html.contains("""class="paste-ui-imagesequence""""), "host class missing")
    assert(html.contains(s"""data-paste-imagesequence="#$mediaId-catalog""""), "catalog reference missing")
    assert(html.contains("""aria-hidden="true""""), "host must not announce frames")
    assert(
      html.contains(s"""<script id="$mediaId-catalog" type="application/json">"""),
      "inert catalog block missing"
    )
    assert(!html.contains("data-scrub-mode"), "no mode declared — attribute must be absent")
    assert(!html.contains("data-scrub-root"), "no root declared — attribute must be absent")
    assert(!html.contains("data-scrub-window"), "no window declared — attribute must be absent")
    assert(!html.contains("data-sequence-intro"), "no intro declared — attributes must be absent")
    assert(!html.contains("<button"), "no intro declared — skip control must be absent")
    assert(!html.contains("poster"), "no poster vocabulary — presentation is authored CSS")
    Seq("data-sequence-from", "data-sequence-to", "data-sequence-scrub-buffer-frames").foreach { attribute =>
      assert(!html.contains(attribute), s"$attribute must be absent when unset")
    }
    assert(
      html.contains("data-paste-progressiveresolution") == sequence.resolution,
      "resolution opt-in must mirror the declared flag"
    )
  }

  private def checkScrubDefaults(html: String): Unit = {
    assert(!html.contains("data-scrub-subject"), "subject is the scope itself unless declared")
    val scoped = render(
      ImageSequence(
        catalog = twoFrame,
        scrub = ScrollScrub.EnterExit.copy(
          subject = Some("#parent-section"),
          windowStart = Some(BigDecimal("0.25")),
          windowEnd = Some(BigDecimal("0.75")),
          mode = Some(ScrubMode.Element),
          root = Some("#hero")
        )
      ),
      "scene"
    )
    assert(scoped.contains("""data-scrub-subject="#parent-section""""), "declared subject missing")
    assert(scoped.contains("""data-scrub-window="0.25 0.75""""), "declared window missing")
    assert(scoped.contains("""data-scrub-mode="element""""), "declared mode missing")
    assert(scoped.contains("""data-scrub-root="#hero""""), "declared root missing")
    val declared = render(
      ImageSequence(
        catalog = twoFrame,
        scrub = ScrollScrub.EnterExit.copy(subject = Some("#declared"))
      ),
      "scene"
    )
    assert(declared.contains("""data-scrub-subject="#declared""""), "declared subject missing")
  }

  private def checkRanges(html: String): Unit = {
    assert(html.contains("""data-sequence-from="1""""), "declared from missing")
    assert(html.contains("""data-sequence-to="0""""), "declared to missing")
    assert(html.contains("""data-sequence-scrub-buffer-frames="2""""), "scrub buffer missing")
  }

  private def checkIntro(html: String, mediaId: String): Unit = {
    assert(html.contains("""data-sequence-intro-trigger="ready""""), "intro trigger missing")
    assert(html.contains("""data-sequence-intro-duration-ms="1200""""), "intro duration missing")
    assert(html.contains("""data-sequence-intro-easing="ease-in-out""""), "intro easing missing")
    assert(html.contains("""data-sequence-intro-from="0""""), "intro from missing")
    assert(html.contains("""data-sequence-intro-to="1""""), "intro to missing")
    assert(html.contains("""data-sequence-intro-buffer-ms="250""""), "intro buffer missing")
    assert(
      html.contains(s"""data-sequence-intro-skip="#$mediaId-skip""""),
      "intro skip reference missing"
    )
    assert(
      html.contains(s"""<button type="button" id="$mediaId-skip" class="paste-ui-imagesequence-skip" hidden>Skip intro</button>"""),
      s"skip control missing or mislabelled:\n$html"
    )
    // First in the scope, ahead of the media: the scene's first tab stop.
    assert(
      html.indexOf(s"""id="$mediaId-skip"""") < html.indexOf(s"""<i id="$mediaId-host""""),
      s"skip control must precede the host:\n$html"
    )
  }

  // The skip control is optional: an intro declared without one keeps
  // every intro attribute and renders no button, no skip reference and no
  // reserved -skip id, in a standalone scene and in a hero alike. The
  // module then plays the intro with scroll, a hidden document, reduced
  // motion, failure or completion as its only ends.
  private def checkIntroWithoutSkip(): Unit = {
    val intro = ImageSequenceIntro(durationMs = 1200, from = Some(0), to = Some(1), skip = false)
    val html = render(ImageSequence(catalog = twoFrame, intro = Some(intro)), "scene")
    assert(html.contains("""data-sequence-intro-trigger="ready""""), s"a skipless intro still declares its trigger:\n$html")
    assert(html.contains("""data-sequence-intro-duration-ms="1200""""), "a skipless intro still declares its duration")
    assert(html.contains("""data-sequence-intro-easing="ease-in-out""""), "a skipless intro still declares its easing")
    assert(html.contains("""data-sequence-intro-to="1""""), "a skipless intro still declares its range")
    assert(!html.contains("data-sequence-intro-skip"), s"a skipless intro must not name a skip control:\n$html")
    assert(!html.contains("<button"), s"a skipless intro must not render a skip control:\n$html")
    assert(!html.contains("scene-skip"), "a skipless intro reserves no -skip id")
    // An unrendered label is not checked.
    val unlabelled = render(
      ImageSequence(catalog = twoFrame, intro = Some(intro.copy(skipLabel = " "))), "scene")
    assert(unlabelled == html, "an unrendered skip label must not change the output")
    val hero = paste.surface.components.html.hero(
      Hero("main", imageSequence = Some(ImageSequence(catalog = twoFrame, intro = Some(intro))))
    )(Html("<h1>Welcome</h1>")).body
    assert(hero.contains("""data-sequence-intro-duration-ms="1200""""), s"a skipless hero intro still declares its duration:\n$hero")
    assert(!hero.contains("data-sequence-intro-skip"), s"a skipless hero intro must not name a skip control:\n$hero")
    assert(!hero.contains("<button"), s"a skipless hero intro must not render a skip control:\n$hero")
    assert(!hero.contains("main-image-skip"), "a skipless hero intro reserves no -skip id")
    // The default declares the skip, exactly as before the field existed.
    assert(ImageSequenceIntro(durationMs = 1200).skip, "an intro declares its skip by default")
  }

  private def checkCatalogJson(html: String): Unit = {
    val catalogJson = jsonBlock(html, "scene-catalog")
    // Key order is part of the rendered bytes: it must be the declared
    // order, identical on every JVM.
    assert(
      catalogJson.startsWith("""{"frames":[{"src":"a-640.jpg","width":640,"height":360,"upgrades":[{"src":"a-1280.jpg","width":1280,"height":720}"""),
      s"catalog JSON must serialize in declared key order:\n$catalogJson"
    )
    // Sorted for the ladder: authored 1920-first rung order serializes ascending.
    val sorted = render(
      ImageSequence(catalog = catalog(
        frame("a-640.jpg", upgrades = Seq(
          ImageRepresentation("a-1920.jpg", 1920, 1080),
          ImageRepresentation("a-1280.jpg", 1280, 720)
        ))
      )),
      "sorted"
    )
    val sortedJson = jsonBlock(sorted, "sorted-catalog")
    assert(
      sortedJson.indexOf("a-1280.jpg") < sortedJson.indexOf("a-1920.jpg"),
      "upgrades must serialize ascending in width"
    )
    // A src containing "</script>" must not terminate the data block.
    val hostile = render(
      ImageSequence(catalog = catalog(frame("</script><img src=x>"))),
      "hostile"
    )
    val hostileJson = jsonBlock(hostile, "hostile-catalog")
    assert(
      !hostileJson.contains("<") && !hostileJson.contains(">"),
      s"catalog JSON must escape markup characters:\n$hostileJson"
    )
    assert(hostileJson.contains("\\u003c/script\\u003e"), "closing tag must serialize escaped")
    assert(
      json.readTree(hostileJson).get("frames").get(0).get("src").asText == "</script><img src=x>",
      "the escaped catalog must round-trip to the authored src"
    )
  }

  private def checkContainerMode(): Unit = {
    // Container mode measures the root's own scroll range: subject and
    // endpoint anchors do not apply, so the renderer omits them rather
    // than emit attributes the element ignores. Root, mode and the
    // fractional window remain the mode's contract.
    val html = render(
      ImageSequence(
        catalog = twoFrame,
        scrub = ScrollScrub(
          mode = Some(ScrubMode.Container),
          root = Some("#scroller"),
          subject = Some("#declared"),
          windowStart = Some(BigDecimal("0.10")),
          windowEnd = Some(BigDecimal("0.90"))
        )
      ),
      "scene"
    )
    assert(html.contains("""data-scrub-mode="container""""), "container mode missing")
    assert(html.contains("""data-scrub-root="#scroller""""), "container root missing")
    assert(html.contains("""data-scrub-window="0.1 0.9""""), "container window missing")
    assert(!html.contains("data-scrub-subject"), "container mode emits no subject")
    assert(!html.contains("data-scrub-start"), "container mode emits no start anchor")
    assert(!html.contains("data-scrub-end"), "container mode emits no end anchor")
  }

  private def checkProgressiveImage(): Unit = {
    val html = paste.surface.components.html.progressiveImage(
      ProgressiveImage(
        mediaId = MediaId("still-sources"),
        base = ImageRepresentation("/images/still-640.webp", 640, 360),
        alt = "Product still",
        upgrades = Seq(ImageRepresentation("/images/still-1280.webp", 1280, 720))
      )
    ).body
    assert(html.contains("""<img src="/images/still-640.webp""""), "baseline src missing")
    assert(html.contains("""width="640"""") && html.contains("""height="360""""), "intrinsic dimensions missing")
    assert(html.contains("""alt="Product still""""), "alt text missing")
    assert(html.contains("data-paste-progressiveresolution"), "opt-in marker missing")
    assert(html.contains("""data-image-sources="#still-sources""""), "sources reference missing")
    assert(
      html.contains("""<script id="still-sources" type="application/json">"""),
      "descriptor block missing"
    )
    val descriptor = jsonBlock(html, "still-sources")
    assert(
      descriptor == """{"src":"/images/still-640.webp","width":640,"height":360,"upgrades":[{"src":"/images/still-1280.webp","width":1280,"height":720}]}""",
      s"descriptor must serialize in declared key order:\n$descriptor"
    )
    // A source carrying markup characters must not terminate the block.
    val hostile = paste.surface.components.html.progressiveImage(
      ProgressiveImage(
        mediaId = MediaId("hostile-sources"),
        base = ImageRepresentation("</script><img src=x>", 640, 360),
        alt = "x",
        upgrades = Seq(ImageRepresentation("sharp.webp", 1280, 720))
      )
    ).body
    val hostileJson = jsonBlock(hostile, "hostile-sources")
    assert(
      !hostileJson.contains("<") && !hostileJson.contains(">"),
      s"descriptor JSON must escape markup characters:\n$hostileJson"
    )
    assert(
      json.readTree(hostileJson).get("src").asText == "</script><img src=x>",
      "the escaped descriptor must round-trip to the authored src"
    )
    // With no sharper rung there is nothing to resolve: a plain img.
    val rungless = paste.surface.components.html.progressiveImage(
      ProgressiveImage(MediaId("plain"), ImageRepresentation("/images/still-640.webp", 640, 360), "x")
    ).body
    assert(!rungless.contains("data-paste-progressiveresolution"), "a rung-less image must not opt in")
    assert(!rungless.contains("<script"), "a rung-less image carries no descriptor")
    // The same discipline on the element attributes: a hostile alt or
    // class reaches the page escaped, never minting a live attribute.
    val hostileAttrs = paste.surface.components.html.progressiveImage(
      ProgressiveImage(
        mediaId = MediaId("hostile-attrs"),
        base = ImageRepresentation("/images/still-640.webp", 640, 360),
        alt = """x" onerror="alert(1)""",
        cssClass = """plain" onload="alert(2)"""
      )
    ).body
    assert(hostileAttrs.contains("x&quot; onerror=&quot;"), "alt must survive escaped")
    assert(hostileAttrs.contains("plain&quot; onload=&quot;"), "class must survive escaped")
    assert(!hostileAttrs.contains(""" onerror="alert"""), "alt must not mint attributes")
    assert(!hostileAttrs.contains(""" onload="alert"""), "class must not mint attributes")
    assert(!html.contains("data-sequence-"), "a standalone image declares no sequence attributes")
    // The typed boundary: nulls and a rung that cannot out-resolve the
    // base are refused at construction.
    Seq(
      () => ProgressiveImage(null, ImageRepresentation("a.webp", 640, 360), "x"),
      () => ProgressiveImage(MediaId("s"), null, "x"),
      () => ProgressiveImage(MediaId("s"), ImageRepresentation("a.webp", 640, 360), null),
      () => ProgressiveImage(
        MediaId("s"), ImageRepresentation("a.webp", 640, 360), "x", upgrades = null),
      () => ProgressiveImage(MediaId("s"), ImageRepresentation("a.webp", 640, 360), "x",
        upgrades = Seq(ImageRepresentation("a.webp", 640, 360))),
      // One literal source claiming a second intrinsic size: the
      // catalog rejects it across frames, and the frame must reject it
      // inside one ladder — the Element's same-source skip would
      // strand the genuine sharper rung behind the shadowing one.
      () => ProgressiveImage(MediaId("s"), ImageRepresentation("a.webp", 640, 360), "x",
        upgrades = Seq(ImageRepresentation("a.webp", 1280, 720))),
      () => frame("a.webp", 640, 360,
        upgrades = Seq(ImageRepresentation("a.webp", 1280, 720))),
      () => ProgressiveImage(MediaId("s"), ImageRepresentation("a.webp", 640, 360), "x",
        upgrades = Seq(ImageRepresentation("b.webp", 320, 180))),
      // The Element requires height to grow with width at every rung.
      () => ProgressiveImage(MediaId("s"), ImageRepresentation("a.webp", 854, 480), "x",
        upgrades = Seq(ImageRepresentation("b.webp", 856, 480)))
    ).foreach(attempt => rejects(attempt(), "a malformed standalone image"))
  }

  private def rejects(configuration: => Unit, what: String): Unit = {
    val rejected = try {
      configuration
      false
    } catch {
      case _: IllegalArgumentException => true
    }
    assert(rejected, s"$what must fail at the model boundary")
  }

  private def checkModelRejects(): Unit = {
    rejects(ImageSequenceCatalog(Seq.empty), "an empty catalog")
    rejects(frame("  "), "a blank src")
    rejects(frame("x.jpg", width = 0), "a zero width")
    rejects(
      frame("x.jpg", upgrades = Seq(ImageRepresentation("x-320.jpg", 320, 180))),
      "an upgrade at or under base width"
    )
    rejects(
      frame("x.jpg", upgrades = Seq(
        ImageRepresentation("x-1280.jpg", 1280, 720),
        ImageRepresentation("x-1280b.jpg", 1280, 960)
      )),
      "duplicate rung widths"
    )
    rejects(
      frame("x.jpg", upgrades = Seq(ImageRepresentation("x-wide.jpg", 2560, 720))),
      "an aspect-mismatched upgrade"
    )
    rejects(
      frame("x.jpg", 854, 480, upgrades = Seq(ImageRepresentation("x-856.jpg", 856, 480))),
      "an upgrade wider but not taller than the base"
    )
    rejects(
      frame("x.jpg", 640, 360, upgrades = Seq(ImageRepresentation("x-642.jpg", 642, 359))),
      "an upgrade wider but shorter than the base"
    )
    // Rounded producer widths of one design ratio stay legal.
    frame("x.jpg", 480, 270, upgrades = Seq(ImageRepresentation("x-1067.jpg", 1067, 600)))
    rejects(frame("\u00A0"), "a src blank by the Element's whitespace rule")
    rejects(frame("a\u0000b.webp"), "a src carrying a control character")
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("0.5")), windowEnd = Some(BigDecimal("0.5"))),
      "an empty window"
    )
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("0.5")), windowEnd = Some(BigDecimal("1.5"))),
      "a window ending past the range"
    )
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("0.99999999999999999999")), windowEnd = Some(BigDecimal("1"))),
      "a window bound the Element's double parse would collapse"
    )
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("1E-20000000")), windowEnd = Some(BigDecimal("1"))),
      "a window bound that expands into an enormous attribute"
    )
    rejects(ImageSequenceIntro(durationMs = 1000, to = Some(-1)), "a negative intro to")
    rejects(ImageSequenceIntro(durationMs = 1000, bufferMs = Some(-1)), "a negative intro buffer")
    // Positional construction reads in declared order: duration, from, to.
    val positional = ImageSequenceIntro(1000, Some(0), Some(1))
    assert(positional.from.contains(0) && positional.to.contains(1), "intro positional order must be from, to")
    rejects(
      ImageRepresentation("x.jpg", 0, 10),
      "a representation without positive dimensions"
    )
    // A null upgrades collection is not an empty ladder — it must fail at
    // the boundary with the model's own rejection, not a follow-on
    // NullPointerException from sorting it.
    rejects(
      frame("x.jpg", upgrades = null),
      "a null upgrades collection"
    )
    rejects(
      catalog(frame("x.jpg", upgrades = null)),
      "a catalog whose frame carries a null upgrades collection"
    )
    rejects(
      frame("x.jpg", upgrades = Seq(null)),
      "a null upgrade entry"
    )
    // The default stays an empty ladder: rejecting null must not over-
    // tighten into requiring rungs.
    assert(frame("x.jpg").sortedUpgrades == Seq.empty, "the default ladder must stay empty, not rejected")
    rejects(ScrollScrub(start = "top"), "a one-edge anchor")
    rejects(ScrollScrub(start = "top left"), "a nonsense anchor token")
    rejects(ScrollScrub(windowStart = Some(BigDecimal("0.5"))), "a half-declared window")
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("0.5")), windowEnd = Some(BigDecimal("0.2"))),
      "an inverted window"
    )
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("-0.1")), windowEnd = Some(BigDecimal("0.5"))),
      "a window starting before the range"
    )
    rejects(ScrollScrub(subject = Some("  ")), "a blank subject selector")
    rejects(ImageSequence(catalog = twoFrame, from = Some(2)), "a from beyond the catalog")
    rejects(ImageSequence(catalog = twoFrame, to = Some(-1)), "a negative to")
    rejects(
      ImageSequence(catalog = twoFrame, intro = Some(ImageSequenceIntro(durationMs = 1000, to = Some(9)))),
      "an intro index beyond the catalog"
    )
    // One resource, one intrinsic size — the same literal source at two
    // different dimensions is the declaration the Element rejects.
    rejects(
      catalog(frame("same.webp", 640, 360), frame("same.webp", 800, 450)),
      "one source claiming two intrinsic sizes"
    )
    rejects(
      catalog(frame(
        "base.webp", 640, 360,
        upgrades = Seq(ImageRepresentation("base.webp", 800, 450))
      )),
      "a base representation disagreeing with its own upgrade"
    )
    // Spellings the browser resolves to one URL are one source.
    rejects(
      catalog(frame("./same.webp", 640, 360), frame("same.webp", 800, 450)),
      "one source spelled two ways claiming two intrinsic sizes"
    )
    rejects(
      catalog(frame(
        "https://cdn.example.com/base.webp", 640, 360,
        upgrades = Seq(ImageRepresentation("HTTPS://CDN.EXAMPLE.COM/x/../base.webp", 1280, 720))
      )),
      "a base and an upgrade spelling one URL"
    )
    // The browser's own aliases of one URL: a %2e segment, a ".." at the
    // root, a default port.
    Seq(
      ("%2e/same.webp", "same.webp"),
      ("/../same.webp", "/same.webp"),
      ("https://cdn.example.com:443/same.webp", "https://cdn.example.com/same.webp")
    ).foreach { case (aliased, plain) =>
      rejects(
        catalog(frame(aliased, 640, 360), frame(plain, 800, 450)),
        s"'$aliased' and '$plain' claiming two intrinsic sizes"
      )
    }
    catalog(frame("../same.webp", 640, 360), frame("same.webp", 800, 450))
    // A ".." removes the empty segment before it, not "x" as well.
    catalog(frame("x//../same.webp", 640, 360), frame("same.webp", 800, 450))
    // Legal reuse: identical dimensions under one source, and distinct
    // sources at whatever dimensions each genuinely carries.
    catalog(frame("same.webp", 640, 360), frame("same.webp", 640, 360))
    catalog(frame("a.webp", 640, 360), frame("b.webp", 800, 450))
    // The typed intro is an actual intro or absent — the module's zero
    // duration convention disables hand-authored intros; the model must
    // never emit attributes for a playback that cannot run.
    rejects(
      ImageSequenceIntro(durationMs = 0),
      "a zero intro duration — None is the no-intro declaration"
    )
    rejects(
      ImageSequenceIntro(durationMs = -1),
      "a negative intro duration"
    )
    // IDs Surface interpolates into generated #id selectors must be plain
    // identifiers — a metacharacter or leading digit binds the wrong
    // element or nothing at all.
    Seq("hero:bad", "scene.part", "1scene", "has space", "#hero").foreach { bad =>
      rejects(MediaId(bad), s"media id '$bad'")
      rejects(Hero(name = bad, imageSequence = Some(ImageSequence(catalog = twoFrame))), s"sequenced hero name '$bad'")
    }
    // Ids resolved by exact id — element ids, link fragments, IDREFs —
    // need only be valid HTML ids.
    Seq("has space", "#hero", "").foreach { bad =>
      rejects(Hero(name = bad), s"hero name '$bad'")
      rejects(NavItem("x", bad), s"nav target '$bad'")
      rejects(NavDisclosure(panelId = bad), s"nav panel id '$bad'")
    }
    Seq("faq.pricing", "2024", "\u00fcber-uns", "a:b").foreach { good =>
      Hero(name = good)
      NavItem("x", good)
      NavDisclosure(panelId = good)
    }
    rejects(NavItem("x", "contact", href = null), "a null nav href")
    rejects(MediaId(""), "an empty media id")
    Hero(name = "hero-main_2")
    MediaId("scene-1")
    NavItem("x", "contact")
    NavDisclosure(panelId = "site-sections")
    // The typed boundary refuses null containers and null contents the
    // same way it refuses malformed values — a null enum instance or a
    // null Option must raise the named IllegalArgumentException at
    // construction, not a NullPointerException inside the template.
    rejects(
      ImageSequenceIntro(durationMs = 1000, easing = null),
      "a null intro easing"
    )
    rejects(
      ImageSequenceIntro(durationMs = 1000, bufferMs = null),
      "a null intro buffer option"
    )
    rejects(
      ImageSequenceIntro(durationMs = 1000, from = null),
      "a null intro from option"
    )
    rejects(ScrollScrub(mode = null), "a null mode option")
    rejects(ScrollScrub(mode = Some(null)), "a null mode value")
    rejects(ScrollScrub(subject = null), "a null subject option")
    rejects(ScrollScrub(subject = Some(null)), "a null subject selector")
    rejects(ScrollScrub(root = Some(null)), "a null root selector")
    rejects(ScrollScrub(windowStart = null), "a null window start option")
    // A Some holding a null bound passes an Option-null check and then
    // dereferences the bound inside the range comparison — the model's
    // named rejection must catch the contents too.
    rejects(
      ScrollScrub(windowStart = Some(null), windowEnd = Some(BigDecimal("0.8"))),
      "a null window start value"
    )
    rejects(
      ScrollScrub(windowStart = Some(BigDecimal("0.2")), windowEnd = Some(null)),
      "a null window end value"
    )
    rejects(
      ImageSequence(catalog = twoFrame, intro = null),
      "a null intro option"
    )
    rejects(
      ImageSequence(catalog = twoFrame, intro = Some(null)),
      "a null intro value"
    )
    rejects(
      ImageSequence(catalog = twoFrame, from = null),
      "a null from option"
    )
    rejects(
      ImageSequence(catalog = twoFrame, scrubBufferFrames = null),
      "a null scrub buffer option"
    )
    rejects(
      Hero(name = "main", imageSequence = null),
      "a null hero sequence option"
    )
    rejects(
      Hero(name = "main", imageSequence = Some(null)),
      "a null hero sequence value"
    )
    rejects(
      ImageSequenceIntro(durationMs = 1000, skipLabel = " "),
      "a blank skip label"
    )
    rejects(
      ImageSequenceIntro(durationMs = 1000, skipLabel = null),
      "a null skip label"
    )
    rejects(
      ImageSequenceIntro(durationMs = 1000, skipLabel = null, skip = false),
      "a null skip label on an intro without a skip"
    )
    // The label is checked only where it renders: an intro without a
    // skip names no control, and turning the skip back on through copy
    // checks the label again.
    val unlabelled = ImageSequenceIntro(durationMs = 1000, skipLabel = " ", skip = false)
    rejects(unlabelled.copy(skip = true), "a blank skip label restored through copy")
    // A label that survives a trim but draws nothing names nothing: wide
    // and zero-width spaces, format characters, a combining mark alone,
    // the Hangul filler, the blank braille pattern, the object
    // replacement character, a noncharacter and a reserved
    // default-ignorable code point.
    Seq(
      "\u00a0", "\u3000", "\u2003", "\ufeff", "\u200b", "\u200b\u00a0\u2060",
      "\u00ad", "\u0301", "\u3164", "\u2800", "\ue000", "\u2028",
      "\ufffc", "\ufdd0", "\uffff", "\u2065", new String(Character.toChars(0xe0080))
    ).foreach { label =>
      val code = label.codePoints().toArray.map(cp => f"U+$cp%04X").mkString(" ")
      rejects(ImageSequenceIntro(durationMs = 1000, skipLabel = label), s"a skip label of $code")
      ImageSequenceIntro(durationMs = 1000, skipLabel = label, skip = false)
    }
    // Any visible character names the control, whatever surrounds it,
    // and so does one this JDK does not assign yet: emoji and ideographs
    // from a later Unicode version than the JDK's draw in a newer engine.
    // The replacement character draws its own box.
    Seq("Skip", "\u00a0Skip\u3000", "\u8df3\u8fc7", "\u23ed", "\u2192", "1", "\u00e9", "e\u0301", "\ufffd")
      .foreach(label => ImageSequenceIntro(durationMs = 1000, skipLabel = label))
    Seq(0x1fae9, 0x1fac6, 0x2ebf0)
      .foreach(cp => ImageSequenceIntro(durationMs = 1000, skipLabel = new String(Character.toChars(cp))))
    rejects(
      ImageSequence(catalog = twoFrame, scrubBufferFrames = Some(-1)),
      "a negative scrub buffer"
    )
  }

  private def checkHeroIntegration(): Unit = {
    // A hero hands its media to the sequence without changing the
    // element's identity: the section becomes the scrub scope, the
    // established image element keeps its id and presentation class as
    // the sequence's host, and no wrapper claims the media id.
    val heroHtml = paste.surface.components.html.hero(
      Hero("main", showImage = true, imageSequence = Some(ImageSequence(catalog = twoFrame)))
    )(Html("<h1>Welcome</h1>")).body
    assert(heroHtml.contains("""<section id="main" class="paste-ui-hero paste-ui-hero-with-image"""),
      s"a sequenced hero is a hero with an image:\n$heroHtml")
    assert(heroHtml.contains("data-paste-scrollscrub"), "hero scrub declaration missing")
    assert(heroHtml.contains("""data-scrub-start="top top""""), "hero scrub start missing")
    assert(heroHtml.contains("""data-scrub-end="bottom top""""), "hero scrub end missing")
    // The scrub declaration belongs to the section, not to a generated
    // wrapper — the host's nearest ancestor-or-self scope is the hero.
    val sectionTag = heroHtml.substring(heroHtml.indexOf("<section"), heroHtml.indexOf('>'))
    assert(sectionTag.contains("data-paste-scrollscrub"), "the section must carry the scrub declaration")
    assert(!heroHtml.contains("paste-ui-imagesequence-scope"), "the hero declares no wrapper scope")
    assert(
      heroHtml.contains("""<i id="main-image"""") &&
        heroHtml.contains("""class="paste-ui-hero-image""""),
      s"the established image element must remain the sequence host:\n$heroHtml")
    assert(heroHtml.contains("""data-paste-imagesequence="#main-image-catalog""""), "hero catalog reference missing")
    assert(!heroHtml.contains("main-image-host"), "no competing host may claim a derived id")
    assert(!heroHtml.contains("paste-ui-imagesequence\""), "no second image layer may compete for the pixels")
    assert(heroHtml.contains("""<script id="main-image-catalog" type="application/json">"""),
      "hero catalog block missing")
    assert(!heroHtml.contains("data-scrub-subject"), "the scope is its own subject unless one is declared")
    assert(!heroHtml.contains("data-paste-parallax"), "a sequenced hero must not declare parallax")
    // Suppressing the attribute is not enough: heroscroll discovers the
    // image by its paste-ui-hero-image class, so the host must declare
    // the established fixed opt-out or the parallax writer paints over
    // the sequence's frame presentation.
    assert(heroHtml.contains("data-hero-image-fixed"),
      s"the sequenced hero host must opt out of legacy parallax writes:\n$heroHtml")
    assert(heroHtml.contains("<h1>Welcome</h1>"), "hero content missing")
    // The image-state class reflects the declared media, not the legacy
    // flag alone — a sequence without showImage is still a hero with an
    // image.
    val sequencedOnly = paste.surface.components.html.hero(
      Hero("main", imageSequence = Some(ImageSequence(catalog = twoFrame)))
    )(Html("<h1>Welcome</h1>")).body
    assert(sequencedOnly.contains("paste-ui-hero-with-image"),
      "an image sequence is image presence even without showImage")
    // An authored scrub subject still declares on the scope carrier.
    val overridden = paste.surface.components.html.hero(
      Hero("main", showImage = true, imageSequence = Some(
        ImageSequence(catalog = twoFrame, scrub = ScrollScrub.EnterExit.copy(subject = Some("#authored")))
      ))
    )(Html("<h1>Welcome</h1>")).body
    assert(overridden.contains("""data-scrub-subject="#authored""""), "authored subject must declare on the hero scope")
    // Without a sequence the ordinary hero contract is unchanged:
    // parallax on the image element, or the authored opt-out's basic
    // image.
    val plain = paste.surface.components.html.hero(
      Hero("main", showImage = true)
    )(Html("<h1>Welcome</h1>")).body
    assert(plain.contains("data-paste-parallax"), "ordinary hero must keep parallax")
    assert(plain.contains("paste-ui-hero-image"), "ordinary hero must keep its image element")
    assert(!plain.contains("data-hero-image-fixed"), "ordinary hero keeps its parallax-writable image")
    // The opt-out belongs to the hero's established host identity — a
    // standalone sequence host carries no heroscroll concern.
    assert(!render(
      ImageSequence(catalog = twoFrame), "scene").contains("data-hero-image-fixed"),
      "a standalone sequence host declares no hero opt-out")
    // A sequence's cssClass lands on the hero's scope root, the section.
    val classed = paste.surface.components.html.hero(
      Hero("main", imageSequence = Some(ImageSequence(catalog = twoFrame, cssClass = "dark-scene")))
    )(Html("<h1>Welcome</h1>")).body
    assert(
      classed.contains("""class="paste-ui-hero paste-ui-hero-with-image dark-scene""""),
      s"a hero must carry its sequence's cssClass on the section:\n$classed")
    // disableParallax keeps the basic image host heroscroll never
    // discovers, so no hero effect runs over the sequence.
    val sequencedNoParallax = paste.surface.components.html.hero(
      Hero("main", disableParallax = true, imageSequence = Some(ImageSequence(catalog = twoFrame)))
    )(Html("<h1>Welcome</h1>")).body
    assert(sequencedNoParallax.contains("""class="paste-ui-hero-basicimage""""), "disableParallax must keep the basic image host")
    assert(!sequencedNoParallax.contains("paste-ui-hero-image\""), "disableParallax must not expose a heroscroll image")
    assert(!sequencedNoParallax.contains("data-hero-image-fixed"), "a basic image host needs no parallax opt-out")
    // A hero intro renders its skip control with the hero-derived ids.
    val heroIntro = paste.surface.components.html.hero(
      Hero("main", imageSequence = Some(ImageSequence(catalog = twoFrame, intro = Some(ImageSequenceIntro(durationMs = 800)))))
    )(Html("<h1>Welcome</h1>")).body
    assert(heroIntro.contains("""data-sequence-intro-skip="#main-image-skip""""), "hero skip reference missing")
    assert(heroIntro.contains("""<button type="button" id="main-image-skip" class="paste-ui-imagesequence-skip" hidden>"""),
      s"hero skip control missing:\n$heroIntro")
    // The skip is the section's first child, ahead of the media and the
    // content: the first tab stop in the hero, so focus handed to the
    // section by a skip continues into the content, never back across it.
    assert(
      heroIntro.indexOf("id=\"main-image-skip\"") < heroIntro.indexOf("<i id=\"main-image\"") &&
        heroIntro.indexOf("<i id=\"main-image\"") < heroIntro.indexOf("id=\"main-content-wrap\""),
      s"hero skip control must precede the media and the content wrap:\n$heroIntro")
    assert(
      heroIntro.substring(heroIntro.indexOf('>', heroIntro.indexOf("<section")) + 1, heroIntro.indexOf("<button")).trim.isEmpty,
      s"hero skip control must be the section's first child:\n$heroIntro")
    assert(heroIntro.indexOf("id=\"main-image-skip\"") == heroIntro.lastIndexOf("id=\"main-image-skip\""),
      s"hero skip control must render once:\n$heroIntro")
    val noParallax = paste.surface.components.html.hero(
      Hero("main", showImage = true, disableParallax = true)
    )(Html("<h1>Welcome</h1>")).body
    assert(noParallax.contains("paste-ui-hero-basicimage"), "parallax opt-out must keep its basic image")
    assert(!noParallax.contains("data-paste-parallax"), "parallax opt-out must suppress the attribute")
    // Model strings are escaped, never turned into new attributes.
    val escaped = render(
      ImageSequence(catalog = twoFrame, cssClass = """dark" onload="alert(1)"""),
      "scene"
    )
    assert(escaped.contains("paste-ui-imagesequence-scope dark&quot;"), "class must survive escaped")
    assert(!escaped.contains(""" onload=""""), "class must not mint attributes")
  }

  // Source identity against the browser's URL parser: pairs that resolve
  // to one URL against every http(s) document base share an identity, and
  // pairs that resolve apart against some base never do.
  private def checkSourceIdentity(): Unit = {
    val identity = ImageRepresentation.sourceIdentity
    Seq(
      "x//../f1.webp" -> "x/f1.webp",
      "./f1.webp" -> "f1.webp",
      "%2e/f1.webp" -> "f1.webp",
      "480/%2E/f1.webp" -> "480/f1.webp",
      "480/.%2e/f1.webp" -> "f1.webp",
      "a/b/.." -> "a/",
      "a/." -> "a/",
      "/../f1.webp" -> "/f1.webp",
      "/a/../../f1.webp" -> "/f1.webp",
      "/.." -> "/",
      " f1.webp " -> "f1.webp",
      "f1\n.webp" -> "f1.webp",
      "./f1.webp?v=2#top" -> "f1.webp?v=2#top",
      "f1.webp?x/../y" -> "./f1.webp?x/../y",
      "https://h.example:443/f1.webp" -> "https://h.example/f1.webp",
      "https://h.example:0443/f1.webp" -> "https://h.example/f1.webp",
      "http://h.example:80/f1.webp" -> "http://h.example/f1.webp",
      "HTTPS://H.Example" -> "https://h.example/",
      "https://h.example?q" -> "https://h.example/?q",
      "https://h.example/a/./b/../f1.webp" -> "https://h.example/a/f1.webp",
      "https://h.example:8443/%2e/f1.webp" -> "https://h.example:8443/f1.webp",
      "wss://h.example:443/s" -> "wss://h.example/s",
      "//CDN.example/x/../f1.webp" -> "//cdn.example/f1.webp",
      "//cdn.example:/f1.webp" -> "//cdn.example/f1.webp"
    ).foreach { case (spelled, plain) =>
      assert(identity(spelled) == identity(plain),
        s"'$spelled' and '$plain' are one URL — identities ${identity(spelled)} and ${identity(plain)}")
    }
    Seq(
      "x//../f1.webp" -> "f1.webp",
      "../f1.webp" -> "f1.webp",
      "../f1.webp" -> "/f1.webp",
      "f1.webp" -> "/f1.webp",
      "/.//f1.webp" -> "//f1.webp",
      ".//f1.webp" -> "/f1.webp",
      "./a:b" -> "a:b",
      "./?q" -> "?q",
      "." -> "?q",
      "a/" -> "a",
      "https://h.example/A.webp" -> "https://h.example/a.webp",
      "http://h.example:443/f1.webp" -> "http://h.example/f1.webp",
      "//h.example:443/f1.webp" -> "//h.example/f1.webp",
      "https:f1.webp" -> "https://f1.webp",
      "f1.webp#a" -> "f1.webp",
      "f1.webp?a" -> "f1.webp",
      "data:image/png,./a" -> "data:image/png,a",
      "https://h.example:99999/f1.webp" -> "https://h.example:34463/f1.webp"
    ).foreach { case (one, other) =>
      assert(identity(one) != identity(other),
        s"'$one' and '$other' resolve apart against some base — both had identity ${identity(one)}")
    }
    // A src the folding does not read compares as written, never as a
    // thrown parse error: an unusable port or host, a query alone, another
    // scheme, and the aliases left to the Element — a backslash, a third
    // slash, a file: path.
    Seq(
      "https://h.example:x/f1.webp", "https://[::1/f1.webp", "//", "https://@/f1.webp", "?",
      "mailto:a@h.example", "a\\..\\f1.webp", "https:///h.example/f1.webp", "file:///a/../f1.webp"
    ).foreach(src => assert(identity(src) == src.trim, s"'$src' must compare as written, was ${identity(src)}"))
  }

  def main(args: Array[String]): Unit = {
    checkSourceIdentity()
    val sequence = ImageSequence(
      catalog = twoFrame,
      from = Some(1),
      to = Some(0),
      intro = Some(ImageSequenceIntro(
        durationMs = 1200,
        from = Some(0),
        to = Some(1),
        bufferMs = Some(250)
      )),
      scrubBufferFrames = Some(2)
    )
    val html = render(sequence, "scene")
    checkStructure(render(ImageSequence(catalog = twoFrame), "plain"), "plain", ImageSequence(catalog = twoFrame))
    checkScrubDefaults(html)
    checkRanges(html)
    checkIntro(html, "scene")
    checkIntroWithoutSkip()
    checkCatalogJson(html)
    checkModelRejects()
    checkContainerMode()
    checkProgressiveImage()

    // CSS class and resolution opt-out flow through.
    val noResolution = render(
      ImageSequence(catalog = twoFrame, cssClass = "site-scene", resolution = false),
      "optout"
    )
    assert(noResolution.contains("paste-ui-imagesequence-scope site-scene"), "authored class missing")
    assert(!noResolution.contains("data-paste-progressiveresolution"), "resolution=false must not opt in")

    // Two sibling sequences with distinct ids cross-bind nothing: every
    // generated reference names the block's own ids exactly.
    val first = render(
      ImageSequence(
        catalog = twoFrame,
        intro = Some(ImageSequenceIntro(durationMs = 500, to = Some(1)))
      ),
      "first-scene"
    )
    val second = render(ImageSequence(catalog = twoFrame), "second-scene")
    assert(first.contains("data-paste-imagesequence=\"#first-scene-catalog\""), "first catalog reference missing")
    assert(first.contains("id=\"first-scene-catalog\""), "first catalog id missing")
    assert(first.contains("data-sequence-intro-skip=\"#first-scene-skip\""), "first skip reference missing")
    assert(first.contains("id=\"first-scene-skip\""), "first skip id missing")
    assert(!first.contains("second-scene"), "the first block references a sibling's id")
    assert(second.contains("data-paste-imagesequence=\"#second-scene-catalog\""), "second catalog reference missing")
    assert(!second.contains("first-scene"), "the second block references a sibling's id")

    checkHeroIntegration()

  }
}
