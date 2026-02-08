# paste-surface-scala

Scala/Twirl implementation of PasteStack Surface Rendering Layer.

## Installation

```scala
// build.sbt
libraryDependencies += "com.pastestack" %% "paste-surface-scala" % "0.1.0"
```

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

MIT
