package com.pastestack.surface.view_models.components.formfield

enum FormSubmission(val cssClass: String) {
  case Native extends FormSubmission("")
  case Apply extends FormSubmission("paste-ui-apply")
  case Html extends FormSubmission("paste-ui-formhtml")
}
