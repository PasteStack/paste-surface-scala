package com.pastestack.surface.view_models.components.formfield

import com.pastestack.surface.view_models.components.GeneratedId

final case class Form(
  action: String,
  method: FormMethod = FormMethod.Post,
  enctype: FormEnctype = FormEnctype.UrlEncoded,
  id: Option[String] = Option.empty,
  submission: FormSubmission = FormSubmission.Native,
  errors: Seq[String] = Seq.empty,
  cssClass: String = "",
  dataAttributes: Map[String, String] = Map.empty
) {
  require(Option(action).exists(_.trim.nonEmpty), "Form action must be nonblank")
  require(method != null, "Form method must be set")
  require(enctype != null, "Form enctype must be set")
  require(submission != null, "Form submission must be set")
  require(submission != FormSubmission.Apply || method == FormMethod.Post, "Apply submission requires POST")
  require(id != null, "Form id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Form id", _))
  DataAttributeName.requireValid(dataAttributes)
}
