package com.pastestack.surface.view_models.components

// The rendered media block's identity: its id, the generated "#id-catalog"
// and (with an intro skip) "#id-skip" references, and the host's catalog
// attribute all derive from this one string, so the generated-selector
// rule applies at construction — a value that cannot resolve as a plain
// #id is refused before any template runs.
final case class MediaId(value: String) {
  GeneratedId.require("MediaId", value)
}
