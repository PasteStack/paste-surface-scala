# paste-surface-scala

Scala/Twirl implementation of PasteStack Surface Rendering Layer.

## Installation

The artifact is published to the PasteStack Maven registry, so a consumer adds that
resolver alongside the dependency:

```scala
// build.sbt
resolvers += "paste-registry" at "https://gitlab.com/api/v4/projects/70289607/packages/maven"

libraryDependencies += "com.pastestack" %% "paste-surface-scala" % "0.1.0"
```

Three coordinates exist, and they are not interchangeable:

| Coordinate | Published by | Use for |
|---|---|---|
| `0.1.0` | a release tag pipeline | anything that ships |
| `0.1.0-develop-latest` | every develop pipeline, moving | tracking unreleased work |
| `0.1.0-develop-<sha>` | every develop pipeline, immutable | reproducing one develop build |

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

Renderer checks use no additional test dependencies:

```sh
sbt 'Test/runMain com.pastestack.surface.MarqueeRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.IncludesRenderingChecks'
sbt 'Test/runMain com.pastestack.surface.ImageSequenceRenderingChecks'
```

They are plain main classes, so `sbt test` does not discover them; CI names them in
the same way.

## Package Structure

```
com.pastestack.surface
├── viewmodels/
│   ├── page/
│   │   └── PageMeta
│   ├── components/
│   │   ├── Hero
│   │   ├── Address
│   │   └── form/
│   │       ├── FormFieldInput
│   │       ├── FormFieldButton
│   │       ├── FormFieldTextarea
│   │       └── HelpToolTip
│   └── assets/
│       ├── CSSInclude
│       └── JSInclude

paste.surface (Twirl templates)
├── layouts/
│   └── webbase.scala.html
├── components/
│   ├── hero.scala.html
│   ├── address.scala.html
│   └── form/
│       ├── formfieldinput.scala.html
│       ├── formfieldbutton.scala.html
│       ├── formfieldtextarea.scala.html
│       └── formfieldhiddenpassthroughs.scala.html
└── assets/
    ├── cssinclude.scala.html
    └── jsinclude.scala.html
```

## License

Apache License, Version 2.0. See `LICENSE` and `NOTICE.md`.
