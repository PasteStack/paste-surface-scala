package com.pastestack.surface.view_models.components.formfield

enum FormMethod(val wireName: String) {
  case Get extends FormMethod("get")
  case Post extends FormMethod("post")
}
