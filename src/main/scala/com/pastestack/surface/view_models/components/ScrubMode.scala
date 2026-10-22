package com.pastestack.surface.view_models.components

enum ScrubMode(val wireName: String) {
  case Element extends ScrubMode("element")
  case Container extends ScrubMode("container")
}
