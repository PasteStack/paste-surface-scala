# paste-surface-scala v0.9.0

**Date:** 2026-10-22

## Added

- Adaptive image sequence surface: `ImageSequenceFrame`,
  `ImageSequenceCatalog`, `ImageRepresentation`, `MediaId`,
  `ScrollScrub`, `ScrubMode`, `IntroEasing` and `ProgressiveImage` view
  models, with `sequenceMedia`, `scrubDeclaration`, `sequenceSkip` and
  `progressiveImage` templates emitting the same declarative markup a
  page could hand-author.
- `AdaptiveImageSequenceManifest` parses the typed manifest contract and
  serializes the catalog into an inert JSON script block, single-owner
  style; malformed manifests reject at the boundary.
- `GeneratedId` validates HTML ids and generated `#` fragment targets;
  `Input`, `Textarea`, `NavItem` and `NavDisclosure` reject null or
  empty ids that would leave labels and toggles bound to nothing.
- A sequenced hero can suppress the legacy parallax writer by emitting
  `data-hero-image-fixed` — sequence frame ownership and background
  presentation no longer compete on the same host.

## Changed

- `ImageSequenceFrame` enforces distinct literal representation sources
  within a frame, so one URL cannot claim multiple intrinsic sizes; the
  catalog-level check still governs reuse across frames.
- `ScrollScrub` window bounds reject null containers and null values
  with `IllegalArgumentException` instead of surfacing an NPE.

## Removed

- `ImageSequencePoster`: the reduced-motion poster is carried on the
  sequence itself.

# paste-surface-scala v0.8.0

**Date:** 2026-10-01

## Added

- `NavItem.href` (`SectionNav.scala`, `sectionNav.scala.html`):
  implements the companion spec addition (v0.8.0). An item's optional
  `href` is emitted verbatim in place of the generated `#` + `targetId`
  fragment, so a nav rendered on a page that does not contain the target
  sections can link to them with real URLs such as `/#approach`.
  Rendering checks cover verbatim emission and fragment suppression.

# paste-surface-scala v0.7.0

**Date:** 2026-09-30

## Added

- `ImageSequence` rendering emits a reduced-motion source carrying
  `posterFinal` ahead of the fallback image when the model supplies no
  responsive poster candidates, so a no-motion reader resolves the
  settled final frame rather than the first. Implements the poster
  fallback branch added in the companion spec (v0.7.0).

## Fixed

- `SiteDocuments` path matching: overlap detection percent-decodes path
  segments before the literal prefix test — `/%63ontact` is `/contact`
  — and `*` and `$` map to their literals instead of comparing as
  wildcards against themselves. A whole-site `Disallow: /` no longer
  emits a meaningless `Allow: /` line beside it.

# paste-surface-scala v0.6.0

**Date:** 2026-09-28

## Added

- `SiteDocuments` (`com.pastestack.surface.documents`): the site-documents
  model — `canonicalOrigin`, `indexablePaths`, `uncrawlablePaths` — enforcing
  its invariants at construction (absolute http(s) origin, slash-prefixed
  paths, no indexable path under a disallow prefix per RFC 9309
  literal-prefix matching) and rendering `robotsTxt`/`sitemapXml`
  deterministically: derived `Sitemap:` line, escaped `<loc>` entries, no
  synthetic `lastmod`. Implements `spec/documents/site-documents.md` from
  the companion spec (v0.6.0).
- `SiteDocumentsDirectives` in the pekko module (`paste-surface-pekko`):
  `robotsRoute`, `sitemapRoute`, and the composed `siteDocumentRoutes`. The
  deployment wraps them in its own baseline response headers.

# paste-surface-scala v0.5.0

**Date:** 2026-09-25

## Changed — breaking

- JAM rendering delegates to the per-render paste-assetgraph page.
  `jambase` takes `page: Option[PastePage]` and `jam: Jam`, records the jam
  once, emits the CSS dump in `<head>` and the JS dump at the end of
  `<body>`, then finishes the page. `CSSInclude`/`JSInclude` keep plain
  `<link>`/`<script>` file includes only; their JAM fields are removed.
- Includes factories (`forPasteJam`, `forForms`, `forCssJam`) return `Jam`
  values carrying the css and js module lists instead of include view
  models.
- `JamIncludes.record(jam, page)` and `JamIncludes.dump(type, page)` take the
  page as an argument; `page.dump()` with no type emits the css markup then
  the js.
- The assetgraph runtime pin moves to the published `0.3.1`, whose page API
  replaces `dumpCss`/`dumpJs` with the scoped `dump` and adds `finish()`.

## Added

- Page finish at the root layout: `page.finish()` after the JS dump fails
  the render naming any jammed module that never reached a dump of its
  type. Includes stay partial renderers; a host-side finish after the
  layout is a no-op.

# paste-surface-scala v0.4.0

**Date:** 2026-09-24

## Added

- `paste-surface-pekko`, a second artifact carrying the Pekko HTTP binding of
  the rendering contract: `RenderingDirectives.rendering` selects page, IO
  reply fragment or JSON from `X-Requested-With` and `Accept` per
  `Rendering.select`; `completeRendered` answers one status for the selected
  rendering with `Vary: X-Requested-With, Accept` always set and `406` when no
  JSON encoder is configured; `renderedResponse` builds the same
  `HttpResponse` for use inside `complete(...)` and rejection handlers. Pekko
  HTTP, pekko-stream and pekko-actor-typed are `Provided`, so the host
  application supplies its own version. The root project aggregates the new
  module, so the published coordinates are `paste-surface-scala` and
  `paste-surface-pekko` at the same version.

# paste-surface-scala v0.3.0

**Date:** 2026-09-23

## Added

- `Form` view model and template: typed `FormMethod` and `FormEnctype`, an
  optional `id`, form-level `errors` rendered as a `role="alert"` list,
  `cssClass`, `dataAttributes`, and `backgroundSubmit`, which emits
  `data-paste-form` for the `paste.ui.form` Element. A blank `action` is
  rejected at construction.
- `IoReplyFragmentTarget` view model and template: the named wrapper an IO
  reply fragment lands in.
- `Presentation`, `Rendering`, `Rendered` and `ViewModelJson`: one view model
  renders as a page, an IO reply fragment, or JSON. `Rendering.select` picks
  JSON when `Accept` lists `application/json` without `text/html` (a `q=0`
  range is not acceptable), else the IO reply fragment on
  `X-Requested-With: XMLHttpRequest`, else the page.
- `IoReplyFragmentEnvelope` success and failure encoders, with
  `IoReplyFragmentMarkup`, `IoReplyFragmentEnvelopeError` and
  `IoReplyFragmentEnvelopeErrorType`.

## Changed

- `Input`, `Button` and `Textarea` reject `dataAttributes` names outside
  `[a-z0-9][a-z0-9._-]*` at construction. Such a name was previously written
  into the element's attribute list as given.
- The pipeline runs the SectionNav, Form and Rendering checks.

# paste-surface-scala v0.2.1

**Date:** 2026-09-22

## Added

- `ImageSequence.fit`, `ImageSequence.scrollWindowStart` and
  `ImageSequence.scrollWindowEnd`, rendered as `data-sequence-fit`,
  `data-sequence-scroll-window-start` and `data-sequence-scroll-window-end`.
  The Element already read all three, so a caller using the renderer had no way
  to choose how frames fit their box or to confine playback to part of the
  target's travel without scripting the rendered element — which the component
  contract forbids. The fields are appended to the model, leaving existing
  positional construction unaffected, and their defaults are the Element's
  defaults, so rendered markup and hand-written markup mean the same thing.
  Construction rejects a fit that is neither `cover` nor `contain`, a window
  bound outside 0 to 1, and an end that does not exceed its start.

## Changed

- The image sequence renderer always emits the fit and scroll window
  attributes, carrying the effective value, instead of omitting them at their
  defaults. Rendered behaviour is unchanged for existing callers, because those
  values are the ones the Element already applied.

# paste-surface-scala v0.2.0

**Date:** 2026-09-22

## Breaking

- `JSInclude` emits the JAM path the server actually serves,
  `/paste/{version}/{module+vN.0~...}{.min}.js`. The previous path inserted a second
  segment, which the server read as part of the first module name — that module was
  silently dropped from the response. `jamChecksum` is gone; `jamModuleVersions`
  carries per-module versions from an asset manifest when the caller has one, and
  `IncludesFactory.AssetConfig` no longer takes a checksum.

## Added

- `Marquee` renderer for the `paste.ui.marquee` Element contract, with renderer
  checks that pin structure, escaping, slot cardinality, and model validation.
- `ImageSequence` renderer: a poster that is readable on its own plus the
  configuration the scroll-driven Element reads, with a `Hero` that hands its
  media to the sequence instead of emitting parallax and the legacy image element.
- `Logo`, `Carousel`, and `SectionNav` scrollspy attributes.
- `SectionNav.disclosure` (`NavDisclosure`): renders a `:target`-driven
  disclosure panel — an opener targeting the panel id, a close link
  targeting the derived `#<panelId>-toggle`, optional footer content and
  accessible labels — with no inline script. Item anchors carry
  `paste-ui-section-nav-link` as a styling hook.
- `IncludesRenderingChecks`, which pins the JAM path shape.

## Changed

- Publishing moves to the magicroot GitLab plugin; the version comes from `VERSION`
  plus the artifact policy's revision, and secure-file credentials are read from
  `.secure_files/.credentials.gitlab`.
- Built on the house Scala 3 line (3.4.2). Consumers on an older 3.x cannot read its
  TASTy.
- CI runs the renderer checks by name; they are plain main classes, so `sbt test`
  alone found nothing and passed.
- `ImageSequence` accepts poster candidates in any order and asks only that their
  widths be distinct; the renderer emits them ascending. The model previously
  rejected unsorted input that the renderer would have sorted anyway.

## Licensing

- Licensed under the Apache License, Version 2.0 (`LICENSE`, `NOTICE.md`).

# paste-surface-scala v0.1.0

**Date:** 2026-02-07

## Initial Release

- Added Hero and Address Twirl components with view models
- Added webbase and jambase layout templates
- Added PagePresenter, PageMeta, AssetInclude view models
- Added HeroFactory, IncludesFactory, PageMetaFactory
- Added FormField components (input, textarea, button, hidden)
- Added SurfaceConfig for HOCON-based configuration
- Restructured `.gitignore` for multi-project shared ignores
