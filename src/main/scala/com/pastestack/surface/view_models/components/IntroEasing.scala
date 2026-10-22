package com.pastestack.surface.view_models.components

enum IntroEasing(val wireName: String) {
  case Linear extends IntroEasing("linear")
  case EaseInOut extends IntroEasing("ease-in-out")
}
