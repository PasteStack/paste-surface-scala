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
renders the declarations for the `paste.ui.carousel` Element.
Its content slot contains `.paste-ui-carousel-item` children; load the carousel
module and stylesheet for discrete paging. It does not duplicate slide content.

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

`SectionNav.smoothscroll` emits `data-paste-smoothscroll` on the `<nav>`,
declaring it a `paste.ui.smoothscroll` container: its section links then move
with smoothscroll's motion and completion history, while a disclosure nav's
toggles and a section chosen from its open panel stay native. Without it,
the default, the links are native fragment navigation.

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
CSS as well (`scroll-behavior`, with a `prefers-reduced-motion` rule).
Give a destination `tabindex="-1"` when it should take focus; the hero renderer
supplies this on its section. The standard UI jam includes smoothscroll, which
moves only links inside a `data-paste-smoothscroll` container
(`SectionNav(smoothscroll = true)` declares it on its `<nav>`); their focus and
`:target` settle as native navigation leaves them, unless the visitor moves
focus during the motion, and every other link is native.

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
scroll in Direct mode, when the document is hidden, under reduced motion, on a failed
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
scrolling to reach it ends a Direct-mode intro. Queued mode preserves a running intro.

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

`Form` renders a `<form>` with a content slot for its fields; `submission` selects `FormSubmission.Native`, `.Apply` or `.Html`. Apply emits
`paste-ui-apply`; Html emits `paste-ui-formhtml`. Load `paste.ui.form.form_apply` for
JSON success/error messages or `paste.ui.form.form_html` for HTML replacement. Both
use the separate `paste.ui.form.form_validity` validity module.
Form-level `errors` render as a `role="alert"` list above the fields.
`IoReplyFragmentTarget` renders the named target an IO reply fragment lands in.

```scala
@paste.surface.components.html.ioReplyFragmentTarget(IoReplyFragmentTarget("feedback")) {
  @paste.surface.components.formfield.html.form(Form(
    action = "/feedback",
    submission = FormSubmission.Html,
    errors = Seq("Try again")
  )) {
    @paste.surface.components.formfield.html.input(Input(name = "email", id = Some("feedback-email"), label = Some("Email")))
  }
}
```

The reply target must be an ancestor of the form; the response replaces its
contents. Without that relationship, submission remains native.

While a transmitted background request is pending, Elements adds
`aria-busy="true"`, `paste-ui-io-html-fragment-busy` and `.paste-ui-formhtml-busy`
to that reply target. Settlement restores the prior aria value, removes the form
busy class and removes the shared busy class if this operation added it, before
failure notification or replay.
Surface renders idle markup; neither `Form` nor `IoReplyFragmentTarget`
pre-renders a busy state. This is one busy cause, and page CSS supplies its
appearance. Unrelated target classes remain intact.

`Input` and `Textarea` labels wrap their controls when no id is supplied; with
an id, labels use `for`/`id`. Help text or server errors require a caller-owned
id. Reserve both that id and `<id>-feedback` uniquely in the page: the renderer
connects the feedback with `aria-describedby` and marks server errors with
`aria-invalid="true"`. Existing help/error calls without an id must add one.
`Form` validates optional ids using the same HTML-id grammar as field controls.

`Form`'s `method` and `enctype` are the `FormMethod` and `FormEnctype` enums.
A blank `action`, a null `method` or `enctype`, a `dataAttributes` name
outside `[a-z0-9][a-z0-9._-]*` (on `Form` and the field components alike),
and a blank or whitespace-carrying IO reply fragment target name are rejected
at construction, including through `copy`.

### Radio state and CSS

`paste.ui.multistate` is a port of the original radio/selected-panel/history module.
Author one radio per `data-multistate-key`/value, associated labels, and optional
panels in ordinary Twirl markup. Several panels may share a pair. Keep controls
and panels as siblings for the original reveal selector:

```css
input[data-multistate-key]:checked ~ [data-multistate-state] { display: block; }
```

Author native `name` attributes and unique label ids. Elements preserves them;
its original GUID-based fallback supplies only missing names and ids. Supply
value-specific `:checked` CSS too when the page must reveal panels without
JavaScript. The complete markup and CSS are in Surface spec's
`spec/elements/multistate.md`.

The `formfield.input` template wraps each input and does not emit this sibling
arrangement or a checked default. Use the direct markup above rather than
adding a new renderer, wrapper or Scala selection model. Surface emits no
runtime selected marker and subscribes to no events. Elements supplies the
`data-multistate-state` and `multistate-state-{key}-{value}` reflection.

Independent selectors use distinct keys and group names. The port does not
provide mirrored strips or form-reset reconciliation. Its public state and
event payload share the current dictionary; the earlier snapshot-copy and
reentrant-change queue contracts are withdrawn. Surface does not promise those
later assistant-added behaviors.

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
IO reply fragment envelope reply (`render`/`success`/`failure`,
`application/json`) that `paste.ui.form.form_autosave` accepts on success; `failure` carries an optional `IoReplyFragmentEnvelopeError`
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
sbt 'Test/runMain com.pastestack.surface.SelectRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.PageMetaRenderingChecks'
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

## Elements compatibility

Elements contract update: autogrow grows only and uses a direct input handler;
`PASTE_FORMS` includes only autogrow (throttle has no forms consumer). A sticky
SectionNav now uses the existing `data-paste-sticky-nav` marker and the first
`data-paste-sticky-target` emitted by `Hero(stickyNavTarget = true)`. Style
`nav.paste-ui-sticky-active`; the root stuck class and custom nav events are
removed. Without a target, the nav uses its initial document position as the
threshold, preserving the supplied original’s fallback.


Fidelity update: `.autogrow` and keyup remain supported. Explicitly loading
`paste.ui.form.form_validity` restores ordinary-form validity reflection and submission
blocking; marked forms retain background submission. The old non-native
validity shim remains retired. `forForms()` still includes only autogrow.

### Original enhancements

Elements also supplies `paste.ui.allowed_chars`, `paste.ui.form.form_autosave`, `imageupload`,
`js_tooltip`, `multistate`, `navigate_on_change`, `prevent_double_submit`,
`select`, `sortable` and `update_text_on_input_change`. Select the required module names
in `Jam(js = Seq(...))`; the dependency graph supplies their dependencies.
The default `forForms()` remains autogrow only.

`Form`, `Input`, `Textarea`, `Select` and `Button` view models carry their
declarations through `dataAttributes`, with the `data-` prefix omitted in map
keys. For example:

```scala
Form(action = "/profile", dataAttributes = Map(
  "ui-autosave" -> "displayName,bio",
  "autosave-action" -> "/profile/draft"
))
Input(name = "reference", pattern = Some("[0-9]+"),
  dataAttributes = Map("allowed-char-expr" -> "[0-9]"))
Button(name = "save", label = "Save",
  dataAttributes = Map("prevent-double-submit" -> "true"))
```

These are markup declarations, not Scala implementations of the behavior.
Author radio sibling layouts, upload companions, tooltip triggers,
sortable slots and text outputs directly in Twirl content slots when the
generic wrapped fields do not express the required structure. Radio names and
IDs stay caller-owned. There is no browser controller in Surface.

`formfield.html.select(Select("region", label = Some("Region"),
dataAttributes = Map("paste-select" -> "")))(options)` emits a native select
and its adjacent `aria-hidden` label mirror inside `.paste-ui-select`.
`options` is ordinary Twirl option/optgroup markup; its `selected` attributes
are the defaults. The Select model also supports id, description, errors,
required, disabled, multiple, size and autocomplete. A label wraps an id-less
control; feedback requires a caller-owned id, as with Input and Textarea.
Load `paste.ui.select` and its stylesheet to reflect the option label for a
custom appearance. Without enhancement the real select remains visible.
Multiple selects and listboxes retain their native appearance. Omitting the
`paste-select` attribute opts out; `forForms()` is unchanged.

Autosave posts native FormData; its listed field names control change detection.
It preserves the immediate first edit/change/blur and fixed ten-second typing
window. `autosaving`, `not-autosaving` and `not-autosaved` reflect its request and
acknowledgement state. Autosave delegates placement to the shared HTML-fragment utility. The existing Scala
`IoReplyFragmentEnvelope` serializer remains a compatible wire-format function;
its `data.fragments` and string variables work unchanged. Autosave also accepts
the original `data.modules`, `data-update-contents` and `data-update-attribute`.
The shared `paste.ui.utilities.io-html-fragment` utility also serves form replies,
explicit lazy loads and marked anchor navigation. Autosave still decides which
responses acknowledge its sent values.

`paste.ui.form.form_html` owns form submission and failure policy and publishes
`paste-ui-formhtml-busy` on the reply target. `prevent_double_submit` owns native
navigation lifetime, preserving submitter values and releasing on reset or
pageshow. The latter leaves canceled background submissions to `form_html`.
Text reflection writes field values only into authored text placeholders;
server reply markup and authored rich tooltip/template HTML remain separate
trusted-markup contracts.

### Metadata and document validation

Both page layouts emit canonical and Open Graph URLs from `PageMeta.canonicalUrl`.
The authored `url` remains available unchanged. Absolute http(s) URLs retain their
scheme; `//host/path` and legacy `host/path` resolve to HTTPS. Empty URLs omit
both tags. Malformed URLs, credentials, unsupported schemes and out-of-range
ports fail construction, including `copy`; international hosts use punycode.
The factory's legacy domain default therefore remains supported.

`SiteDocuments` rejects literal or percent-encoded `.` and `..` path segments in
both indexable and disallowed paths before checking overlap. Envelope variable
names reject Java whitespace, Unicode space characters and U+FEFF, plus `,` and
`=`, so JavaScript binding-name trimming cannot change a serialized key.

The CI check list runs all ten Surface check programs listed above, including
`SelectRenderingChecks` and `PageMetaRenderingChecks`, alongside the Pekko tests.

## License

Apache License, Version 2.0. See `LICENSE` and `NOTICE`.


The restoration separates `FormSubmission.Native`, `Apply` and `Html`. Migrate
old `backgroundSubmit = true` callers to `submission = FormSubmission.Html`
and their explicit JAM from `paste.ui.form.form_validity` to `paste.ui.form.form_html`; update
`paste.ui.form:failed` listeners and `paste-ui-form-busy`/`-failure` styles to
`formhtml`. The transport configuration attributes remain compatible.

`LazyContent(url, id, cssClass)` renders `data-lazy-load` and a fallback slot.
Call `paste.ui.lazy_loader.load_visible_content(parent)` after showing content,
or the original all/single APIs as appropriate. Rendering is not an automatic
fetch request. Its visibility test is layout size, not viewport intersection.

`IncludesFactory.defaultIncludes()` includes smoothscroll, which moves only
links inside a declared `data-paste-smoothscroll` container; every other
same-document link is native fragment navigation. `Jam` can also
request an app-owned external-library wrapper such as `libs.jquery`; the
wrapper must be in the asset graph. No external-library ViewModel is necessary.

## Form family module names

Use `paste.ui.form.form_validity` for native validity reflection,
`paste.ui.form.form_apply` for JSON submissions, and `paste.ui.form.form_html`
for HTML reply replacement. Apply and HTML include validity as a dependency.
Choose one submission owner per form. `FormSubmission` and its emitted HTML
remain unchanged: `Native`, `Apply`, and `Html` describe behavior, not file paths.

Load the canonical modules directly. The former top-level form, apply,
formhtml and autosave JavaScript entries have been removed; no compatibility
includes remain. Autosave is `paste.ui.form.form_autosave`, at
`modules/form/form_autosave/form_autosave.js`, with its own trigger and request
lifetime. Its request deadline defaults to 30 seconds, overridden per form by
`data-autosave-timeout`. Forms still require explicit
autosave declarations; loading the module does not opt ordinary forms in.

Existing classes, data attributes and `paste.ui.formhtml:failed` keep their
spelling. The general form stylesheet and field mixins remain in `modules/form`.
Import Apply styles from `modules/form/form_apply/form_apply.scss` or its silent
`_mixin.scss`; the former `modules/apply` forwarding styles have been removed.

## Ordered sequence playback

`ImageSequence(..., playback = SequencePlayback.Queued(30))` preserves a running
intro, then traverses from the authored scroll-range start to the latest scroll
destination without dropping intermediate catalog indices. The rate is an integer
from 1 to 120, defaulting to 30. `SequencePlayback.Direct` is the default and emits
no new attributes. Queued emits `data-sequence-playback="queued"` and
`data-sequence-playback-fps`. Rendering adds no listeners or runtime state.

Input coalesces to the latest destination; travel continues after input stops
and reverses from the current presentation. Readiness and resource limits still
apply; a missing frame holds the last usable image. Intro completion has a
bounded extension of 15 seconds beyond its duration. Explicit skip, reduced
motion, hidden-page and history rules retain their existing priority. A scroll
during startup waiting still cancels the intro. Native scrolling and CSS layout
remain authoritative; this mode does not pin content in view.

Queued playback continues from a completed intro endpoint, including intervening
catalog frames when that endpoint differs from the scroll range start. Between
due playback frames it sleeps; new input can update the destination immediately.

## Rendered fragments beyond forms

`IoReplyFragmentTarget(name, cssClass, format)` renders the existing named target
plus `data-paste-io-reply-fragment-format="html|json"`; format defaults to
`IoReplyFragmentFormat.Html`. `Json` explicitly requests a wrapped response.
`IoReplyFragmentLink(href, target, cssClass)` and
`paste.surface.components.html.ioReplyFragmentLink(link)(content)` render an
ordinary anchor with the target declaration. Load `paste.ui.fragment_navigation`
for that optional navigation behavior. The same href must render a full page
for ordinary navigation. Neither type opts forms into autosave.
After validating a reply, navigation commits the destination URL before inserting
markup so native relative resources resolve correctly. A refused DOM write falls
back to the full page in that same history entry.

Use `IoReplyFragmentEnvelope.render(status, fragments, variables, time, title)`
for JSON containing rendered markup at the actual response status, including
422 validation responses. `title: Option[String]` updates an enhanced article
navigation's document title. `success` still produces the 200 envelope; `failure`
is a metadata-only error, without renderable markup. All helpers escape JSON;
js/css lists remain metadata and do not load assets. The `Presentation` encoder
can call this function with the same fragment template used by its HTML branch.
Negotiated responses retain `Vary: X-Requested-With, Accept`; endpoint cache
policy stays with the application.

See [the rendering contract](../paste-surface-spec/spec/rendering/rendering.md)
for response scope, ownership, navigation, lifecycle and native fallbacks.
