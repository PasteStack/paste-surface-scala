# paste-surface-scala

Scala/Twirl implementation of PasteStack Surface Rendering Layer.

## Installation

The artifact is published to the PasteStack Maven registry, so a consumer adds that
resolver alongside the dependency:

```scala
// build.sbt
resolvers += "paste-registry" at "https://gitlab.com/api/v4/projects/70289607/packages/maven"

libraryDependencies += "com.pastestack" %% "paste-surface-scala" % "0.3.0"
```

Three coordinates exist, and they are not interchangeable:

| Coordinate | Published by | Use for |
|---|---|---|
| `0.3.0` | a release tag pipeline | anything that ships |
| `0.3.0-develop-latest` | every develop pipeline, moving | tracking unreleased work |
| `0.3.0-develop-<sha>` | every develop pipeline, immutable | reproducing one develop build |

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
  @paste.surface.components.formfield.html.input(Input(name = "email", label = Some("Email")))
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

Renderer checks use no additional test dependencies:

```sh
sbt 'Test/runMain com.pastestack.surface.MarqueeRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.IncludesRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.ImageSequenceRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.SectionNavRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.FormRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.RenderingChecks'
sbt 'pekko/Test/runMain com.pastestack.surface.pekko.RenderingDirectivesChecks'
```

They are plain main classes, so `sbt test` does not discover them; CI names them in
the same way.

### Pekko HTTP

`paste-surface-pekko` carries the Pekko HTTP binding over `Presentation`: the
`rendering` directive selects per `Rendering.select`, and `completeRendered`
answers one status for the selected rendering with `Vary: X-Requested-With,
Accept` always set. A JSON request without an encoder answers `406`.
`renderedResponse` produces the same `HttpResponse` inside `complete(...)` for
rejection handlers.

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
│   │   ├── Hero, Address, SectionNav, Marquee, Logo, Carousel, ImageSequence, IoReplyFragmentTarget
│   │   └── formfield/
│   │       ├── Input, Button, Textarea, Hidden, HelpToolTip
│   │       └── Form, FormMethod, FormEnctype, DataAttributeName
│   └── includes/
│       ├── CSSInclude
│       └── JSInclude
├── view_model_factories/
│   ├── PageMetaFactory, HeroFactory, IncludesFactory
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
│   ├── imagesequence.scala.html, ioReplyFragmentTarget.scala.html
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
