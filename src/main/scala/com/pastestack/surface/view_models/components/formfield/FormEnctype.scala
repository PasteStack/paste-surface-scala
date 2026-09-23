package com.pastestack.surface.view_models.components.formfield

enum FormEnctype(val wireName: String) {
  case UrlEncoded extends FormEnctype("application/x-www-form-urlencoded")
  case Multipart extends FormEnctype("multipart/form-data")
}
