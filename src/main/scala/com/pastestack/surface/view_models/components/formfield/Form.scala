package com.pastestack.surface.view_models.components.formfield

final case class Form(
  action: String,
  method: FormMethod = FormMethod.Post,
  enctype: FormEnctype = FormEnctype.UrlEncoded,
  id: Option[String] = Option.empty,
  backgroundSubmit: Boolean = false,
  errors: Seq[String] = Seq.empty,
  cssClass: String = "",
  dataAttributes: Map[String, String] = Map.empty
) {
  require(Option(action).exists(_.trim.nonEmpty), "Form action must be nonblank")
  require(method != null, "Form method must be set")
  require(enctype != null, "Form enctype must be set")
  DataAttributeName.requireValid(dataAttributes)
}
