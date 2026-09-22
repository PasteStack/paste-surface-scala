# Unreleased

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
