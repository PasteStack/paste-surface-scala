# paste-surface-scala

Scala/Twirl implementation of PasteStack Surface Rendering Layer.

## Installation

The artifact is published to the PasteStack Maven registry, so a consumer adds that
resolver alongside the dependency:

```scala
// build.sbt
resolvers += "paste-registry" at "https://gitlab.com/api/v4/projects/70289607/packages/maven"

libraryDependencies += "com.pastestack" %% "paste-surface-scala" % "0.8.0"
```

Three coordinates exist, and they are not interchangeable:

| Coordinate | Published by | Use for |
|---|---|---|
| `0.8.0` | a release tag pipeline | anything that ships |
| `0.8.0-develop-latest` | every develop pipeline, moving | tracking unreleased work |
| `0.8.0-develop-<sha>` | every develop pipeline, immutable | reproducing one develop build |

There is no `-SNAPSHOT` coordinate. A pin left on one resolves to whatever was
published before this versioning, which is not this source.

Built for the house Scala 3 line (3.4.2); consumers on an older 3.x cannot read its
TASTy.

## Usage

### ViewModels

```scala
import com.pastestack.surface._

val pageMeta = PageMeta(
  siteName = "My Site",
  title = "Home",
  description = "Welcome to my site",
  themeColor = "336699"
)

val hero = Hero(
  name = "main",
  showImage = true,
  stickyNavTarget = true
)

val address = Address(
  street1 = "123 Main St",
  locality = "Springfield",
  region = "IL",
  postalCode = "62701",
  countryCode = "USA"
)
```

### Construction checks

View models reject ids that cannot render correctly with an
`IllegalArgumentException`, at construction and through `copy`. Releases up to
0.8.0 accepted these and rendered them as given, so existing call sites can
start failing:

- `Hero` is a `final case class`; compose it rather than subclass it. Its
  `name` becomes the section id other markup links to, so it must be nonempty,
  without whitespace or a leading `#`. A hero carrying an `imageSequence`
  needs a plain identifier (`^[A-Za-z_][A-Za-z0-9_-]*$`), since Surface derives
  selectors from it.
- `NavItem.targetId` follows the same rule whenever the item renders the
  generated `#targetId` link. `NavItem("Top", "")`, which rendered `href="#"`,
  is rejected: write `NavItem("Top", "", href = Some("#"))` to keep that link.
  An item with an explicit `href` is not checked; a null `href` is rejected.
- `NavDisclosure.panelId` follows the same rule.
- `Input.id` and `Textarea.id`, when present, must be nonempty and free of
  whitespace, HTML's own id rule. A leading `#` is allowed there, because a
  field id is only ever paired with its label's `for`, never written after a
  `#`.

Ids a component derives from these names are reserved on the page: a hero
named `main` also emits `main-image`, `main-content-wrap` and `main-content`,
and [Image sequence](#image-sequence) and
[Section navigation](#section-navigation) list theirs. Surface checks each
id's syntax, not its uniqueness across the page.

### Templates

```scala
// In your Twirl template
@import paste.surface.layouts.html.webbase
@import paste.surface.components.html.hero
@import com.pastestack.surface._

@webbase(pageMeta) {
  @* headContent *@
  <link rel="stylesheet" href="/css/main.css">
} {
  @* navContent *@
  <nav>...</nav>
} {
  @* mainContent *@
  @hero(Hero("main", showImage = true)) {
    <h1>Welcome</h1>
  }
} {
  @* footerContent *@
  <p>&copy; 2025</p>
} {
  @* scriptIncludes *@
  <script src="/js/main.js"></script>
}
```

### Marquee

The renderer emits the `paste.ui.marquee` Element's contract: one labelled native list, one
original list item per trusted HTML slot, and a hidden/disabled pause button until enhancement is
available. Load the Element's CSS and JavaScript through the normal asset bootstrap. Surface
renders markup only; it has no carousel runtime or duplicate-content mechanism.

```scala
@import com.pastestack.surface.view_models.components.Marquee

@paste.surface.components.html.marquee(Marquee(
  label = "Partners",
  durationSeconds = 45.0,
  pauseLabel = "Pause motion",
  resumeLabel = "Resume motion"
))(items)
```

`items` is `Seq[play.twirl.api.Html]` supplied by caller templates; escape untrusted text before
constructing the slots. Blank accessible labels and non-positive/nonfinite durations are rejected.
The Element keeps all originals in static flow without JavaScript, with reduced motion, while
paused, with insufficient repeat coverage, or with interactive content. See the Marquee contract
in `paste-surface-spec/spec/view_models/components.md`.

### Section navigation

`SectionNav.disclosure` (`NavDisclosure`) renders the Elements disclosure
contract instead of the plain list: the panel carries `panelId`, the open link
targets `#panelId`, and the close link targets the derived `#<panelId>-toggle`
id. Disclosure state is the URL fragment — the `paste.ui.sectionnav`
stylesheet owns presentation and no JavaScript module is involved. `panelId`
and the derived `#<panelId>-toggle` id must both be unique per page.

```scala
@paste.surface.components.html.sectionNav(SectionNav(
  items = Seq(NavItem("Overview", "overview"), NavItem("Details", "details")),
  disclosure = Some(NavDisclosure(panelId = "site-sections"))
))
```

Where a section lands below a sticky nav is page CSS; Surface renders none.
The `paste.ui.sectionnav` stylesheet supplies it: a zero-specificity
`:where(:target) { scroll-margin-top: var(--paste-ui-section-nav-clearance, 51px) }`
rule, where 51px is the mixin's default nav height plus its bottom border. The
browser applies it whenever it scrolls to a fragment, so a section link lands
the section just below the nav. When a fixed site header sits above the stuck
nav, set the custom property to the total of both, or sections land under the
header:

```css
:root { --paste-ui-section-nav-clearance: calc(3.5rem + 51px); }
```

A page that clears its chrome with `scroll-padding-top` instead sets the
custom property to `0px`, since the two add. A page without that stylesheet
sets `scroll-margin-top` on its targets, or `scroll-padding-top` on the
scrolling element, itself. Smooth motion is page
CSS as well (`scroll-behavior`, with a `prefers-reduced-motion` rule); the
`paste.ui.smoothscroll` Element only moves keyboard focus to the target once
the browser's own fragment navigation lands.

### Image sequence

`ImageSequence` renders the `paste.ui.imagesequence` Element's contract: one
scope carrying the declared `ScrollScrub` relationship, a host element
pointing at an inert JSON `ImageSequenceCatalog`, and an optional
`ImageSequenceIntro` for the finite intro, with an authored skip control
unless the intro sets `skip = false`.

```scala
// Application wiring, once per manifest and base — never per request:
val featureCatalog: ImageSequenceCatalog =
  AdaptiveImageSequenceManifest.parseJson(manifestJson).toCatalog(siteResolver)
```

```scala
// In the page template, which receives the prepared catalog:
@paste.surface.components.html.imagesequence(
  ImageSequence(
    catalog = featureCatalog,
    scrub = ScrollScrub.TopExit,
    intro = Some(ImageSequenceIntro(durationMs = 1200, to = Some(23), easing = IntroEasing.EaseInOut))
  ),
  MediaId("feature-sequence")
)
```

An intro starts only while the scene's scroll progress is still 0, its authored
start. Pair it with a range that starts where the page opens: `TopExit` on a
scene in the first viewport, as here. Under `EnterExit`, progress 0 means the
subject has not yet entered the viewport, so a scene already in view ends its
intro at once, and one below the fold can only play it out of sight.

`MediaId` is the validated rendered-block identity — every `#id` the template
emits is a plain identifier by construction. `mode` is the `ScrubMode` enum,
`easing` the `IntroEasing` enum; each case carries the Element's wire value.
Selectors (`root`, `subject`) stay caller-owned strings —
Surface refuses blank or null ones and emits valid values verbatim. Under
`ScrubMode.Container` the scope omits the subject and endpoint attributes —
the container's own scroll range is the whole contract. A `Hero` carrying an
`imageSequence` keeps its established image element (`{name}-image` with the
`paste-ui-hero-image` class, or `paste-ui-hero-basicimage` under
`disableParallax`) as the sequence host, and the hero section itself carries
the scrub declaration and the sequence's `cssClass`.

The baseline image is authored CSS on the host — the renderer emits no poster
vocabulary — so required baseline and reduced-motion styling are media
conditions the page's stylesheet owns (for example a plain final frame under
`prefers-reduced-motion`). A tiered producer manifest converts to the
canonical catalog through `AdaptiveImageSequenceManifest`; resolution of raw
manifest srcs into URLs is the consuming site's policy, passed to `toCatalog`.
A catalog serializes its JSON once, so build it at wiring time and reuse it.

The intro's skip control is an ordinary `<button class="paste-ui-imagesequence-skip" hidden>`
the intro reveals while it plays. Surface emits it as the scope's first child
(the hero section's, in a hero), ahead of the media and the content, so it is
the first tab stop in the scene. A skip hands focus to the scope, and the next
Tab continues into the content rather than crossing it a second time.

The skip control is optional. `ImageSequenceIntro(..., skip = false)` renders
no button and no `data-sequence-intro-skip` attribute; the intro still ends on
any scroll, when the document is hidden, under reduced motion, on a failed
load, or when it completes. `skip` defaults to `true`, so existing intros
render unchanged, and `skipLabel` is checked only when a skip renders.

Surface ships no presentation for the button, so the page positions it out of
flow over the media and clear of any fixed header. Without such a rule a
revealed skip takes a line at the top of the scope and pushes the content down
until the intro ends. For example, under a fixed header `4rem` tall that
overlays the top of the hero:

```css
.paste-ui-hero, .paste-ui-imagesequence-scope { position: relative; }
.paste-ui-imagesequence-skip {
  position: absolute;
  top: calc(4rem + 1rem);
  right: 1rem;
  z-index: 1;
}
```

Anchor it to an edge that stays on screen while the intro plays, as the top
of the hero does (without a fixed header, `top: 1rem`): a hero taller than a
phone's visible viewport puts a bottom-anchored skip below the fold, and
scrolling to reach it ends the intro.

Every id a sequence derives is reserved on the page. An `imagesequence` with
`MediaId("scene")` emits `scene`, `scene-host`, `scene-catalog` and, with an
intro that has a skip, `scene-skip`. A sequenced hero named `main` emits
`main`, `main-image`, `main-image-catalog`, `main-content-wrap` and
`main-content`, plus `main-image-skip` with an intro that has a skip. Two
blocks that derive the same id break each other: the Element refuses both
sequences that name one skip control, and each stays at its baseline.

Migration from the earlier `ImageSequence` model: `source` and the poster
fields (`posterFirst`, `posterFinal`, `posterWidth`, `posterHeight`,
`posterSources`/`ImageSequencePoster`, `posterSizes`) are replaced by an
`ImageSequenceCatalog` plus a baseline image authored in the page's CSS —
including a `prefers-reduced-motion` rule where a settled frame was served
before. `range` becomes `scrub = ScrollScrub.TopExit` or
`ScrollScrub.EnterExit`; `scrollWindowStart`/`scrollWindowEnd` become the
`ScrollScrub` window; `scrollFrom`/`scrollTo` become `from`/`to`;
`progressTargetId` becomes `scrub.subject`; `fit` becomes CSS
`background-size`. `preload`, `priority` and the intro's `mode` and `trigger`
are retired. `ImageSequenceIntro.toFrame` becomes `to`, and `None` now means
the last catalog index rather than the scroll range's start, so set it
explicitly when migrating. The template takes a `MediaId` in place of the two
id strings, and a sequenced hero's `name` must be a plain identifier.

`ProgressiveImage` renders the standalone half of the resolution contract: an
ordinary `<img>` whose authored `src`, `width`, `height` and `alt` are the
working baseline, plus a `data-image-sources` reference to an inert
`script[type="application/json"]` descriptor of sharper rungs — the
`paste.ui.progressiveresolution` Element reads it and honours the
`--paste-resolution-max-width` CSS policy. Use native `srcset`/`picture`
selection instead where it already covers the requirement.

```scala
@paste.surface.components.html.progressiveImage(
  ProgressiveImage(
    mediaId = MediaId("still-sources"),
    base = ImageRepresentation("/images/still-640.webp", 640, 360),
    alt = "Product still",
    upgrades = Seq(ImageRepresentation("/images/still-1280.webp", 1280, 720))
  )
)
```

### Form and IO Reply Fragment Target

`Form` renders a `<form>` with a content slot for its fields; `backgroundSubmit`
emits `data-paste-form` so the `paste.ui.form` Element submits in the background.
Form-level `errors` render as a `role="alert"` list above the fields.
`IoReplyFragmentTarget` renders the named target an IO reply fragment lands in.

```scala
@paste.surface.components.formfield.html.form(Form(
  action = "/feedback",
  backgroundSubmit = true,
  errors = Seq("Try again")
)) {
  @paste.surface.components.formfield.html.input(Input(name = "email", id = Some("feedback-email"), label = Some("Email")))
}

@paste.surface.components.html.ioReplyFragmentTarget(IoReplyFragmentTarget("feedback")) {
  <p>Thanks</p>
}
```

`Form`'s `method` and `enctype` are the `FormMethod` and `FormEnctype` enums.
A blank `action`, a null `method` or `enctype`, a `dataAttributes` name
outside `[a-z0-9][a-z0-9._-]*` (on `Form` and the field components alike),
and a blank or whitespace-carrying IO reply fragment target name are rejected
at construction, including through `copy`.

### Rendering a view model

A presenter builds one view model per request outcome; `Presentation` renders it
as a full page, an IO reply fragment, or JSON, and `Rendering.select` chooses
from the request headers — `Accept` listing `application/json` as acceptable
without `text/html` (a `q=0` range is not acceptable) picks JSON, else
`X-Requested-With: XMLHttpRequest` picks the IO reply fragment, else the page.
One outcome carries one status code in every rendering, and every negotiated
response carries `Vary: X-Requested-With, Accept` (`Rendering.VaryHeader`).

```scala
val presentation = Presentation[MyViewModel](
  page = vm => mypage.html(vm),       // layout + IO reply fragment template
  ioReplyFragment = vm => ioReplyFragment.html(vm),
  json = Some(vm => myJson.encode(vm))
)

presentation.render(model, Rendering.select(xRequestedWith, accept)) match {
  case Some(rendered) => // respond rendered.body as rendered.contentType
  case None           => // Json requested without an encoder: respond 406
}
```

JSON is the view model encoded by an encoder the consumer supplies — Surface
does not choose a JSON library; a JSON request with no encoder renders `None`,
which the application answers `406`. `IoReplyFragmentEnvelope` encodes the
separate IO reply fragment envelope reply (`success`/`failure`,
`application/json`) the `paste.ui.io-reply-fragment-envelope` Element applies
only on success; `failure` carries an optional `IoReplyFragmentEnvelopeError`
(error type plus detail) for the `error_type`/`error_code`/`error_detail`
fields.

### JAM includes

Includes declare JAM module names only; the markup — URL, dependency
expansion, inlining — is rendered by the per-render `PastePage` from
`paste-assetgraph-runtime`, which the application owns via its `Paste`
instance. The `jambase` layout owns the contract end to end: it records the
`Jam` module set on a fresh `assetgraph.page()` per render, dumps CSS into
`<head>` and JS at the bottom of `<body>` (a repeated dump of one type skips
the modules that type's earlier dumps already delivered), and finishes the
page. The plain `css`/`js` include templates take one argument and remain for
direct includes.

```scala
// App-owned, created once at startup:
val assetgraph = Paste.fromClasspath()

// In the page template, a fresh assetgraph.page() per render arrives as a
// parameter:
@(meta: PageMeta, cssIncludes: CSSInclude, jsIncludes: JSInclude, jam: Jam, page: PastePage)

@paste.surface.layouts.html.jambase(
  meta,
  cssInclude = cssIncludes,
  jsInclude = jsIncludes,
  page = Some(page),
  jam = jam
)()() {
  @* mainContent *@
  <main>...</main>
}()()
```

Migration: `jamVersion`, `jamModuleVersions` and `JSInclude.jamUrl` are
removed — the surface composes no JAM URL. `IncludesFactory.forPasteJam` takes
only `additionalModules`, `forForms` takes none, `AssetConfig` carries only
`debugMode`, and `assets.version` is gone from `SurfaceConfig`. JAM-mode
includes now need `Some(assetgraph.page())` at render time.

Renderer checks use no additional test dependencies:

```sh
sbt 'Test/runMain com.pastestack.surface.MarqueeRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.IncludesRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.AdaptiveImageSequenceManifestChecks'
sbt 'Test/runMain com.pastestack.surface.ImageSequenceRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.SectionNavRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.FormRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.RenderingChecks'
sbt 'Test/runMain com.pastestack.surface.SiteDocumentsChecks'
```

They are plain main classes, so `sbt test` does not discover them; CI names them in
the same way. The pekko binding's checks are a ScalaTest suite instead, run
through `testOnly`:

```sh
sbt 'pekko/testOnly com.pastestack.surface.pekko.RenderingDirectivesChecks'
```

### Pekko HTTP

`paste-surface-pekko` carries the Pekko HTTP binding over `Presentation`: the
`rendering` directive selects per `Rendering.select`, and `completeRendered`
answers one status for the selected rendering with `Vary: X-Requested-With,
Accept` always set. A JSON request without an encoder answers `406`.
`renderedResponse` is the synchronous form — it renders in place for
rejection handlers, so a caller wrapping it in `complete(...)` must run that
`Future` on an explicit blocking dispatcher; prefer `completeRendered`, which
does so itself on the blocking-io dispatcher.

```scala
libraryDependencies += "com.pastestack" %% "paste-surface-pekko" % "<version>"
```

```scala
import com.pastestack.surface.pekko.RenderingDirectives.*
import org.apache.pekko.http.scaladsl.server.Directives.*

val route =
  path("feedback" / Segment) { requestId =>
    rendering { r =>
      completeRendered(StatusCodes.OK, presentation, model, r)
    }
  }
```

Pekko is a `Provided` dependency: the host application brings its own
pekko-http, pekko-stream and pekko-actor-typed, so the binding never forces a
version on it.

## Package Structure

```
com.pastestack.surface
├── view_models/
│   ├── page/
│   │   └── PageMeta
│   ├── components/
│   │   ├── Hero, Address, SectionNav, NavItem, NavDisclosure, Marquee, Logo, Carousel
│   │   ├── ImageSequence, ImageSequenceCatalog, ImageSequenceFrame, ImageRepresentation
│   │   ├── ImageSequenceIntro, ScrollScrub, ScrubMode, IntroEasing, MediaId
│   │   ├── ProgressiveImage
│   │   ├── IoReplyFragmentTarget
│   │   └── formfield/
│   │       ├── Input, Button, Textarea, Hidden, HelpToolTip
│   │       └── Form, FormMethod, FormEnctype, DataAttributeName
│   └── includes/
│       ├── CSSInclude
│       └── JSInclude
├── view_model_factories/
│   ├── PageMetaFactory, HeroFactory, IncludesFactory, AdaptiveImageSequenceManifest
├── presenters/
│   └── PagePresenter
├── rendering/
│   ├── Rendering, ViewModelJson, Rendered, Presentation
│   └── IoReplyFragmentEnvelope (IoReplyFragmentEnvelopeErrorType, IoReplyFragmentEnvelopeError, IoReplyFragmentMarkup)
└── config/
    └── SurfaceConfig

paste.surface (Twirl templates)
├── layouts/
│   ├── webbase.scala.html
│   └── jambase.scala.html
├── components/
│   ├── hero.scala.html, address.scala.html, sectionNav.scala.html
│   ├── marquee.scala.html, logo.scala.html, carousel.scala.html
│   ├── imagesequence.scala.html, sequenceMedia.scala.html, sequenceSkip.scala.html
│   ├── scrubDeclaration.scala.html
│   ├── progressiveImage.scala.html, ioReplyFragmentTarget.scala.html
│   └── formfield/
│       ├── input.scala.html, button.scala.html, textarea.scala.html
│       ├── hidden.scala.html, form.scala.html
└── includes/
    ├── css.scala.html
    └── js.scala.html

com.pastestack.surface.pekko (paste-surface-pekko)
└── RenderingDirectives
```

## License

Apache License, Version 2.0. See `LICENSE` and `NOTICE.md`.
