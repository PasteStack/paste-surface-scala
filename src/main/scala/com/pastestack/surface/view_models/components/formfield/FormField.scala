package com.pastestack.surface.view_models.components.formfield

sealed trait FormField {
  def name: String
}

case class Input(
  name: String,
  inputType: String = "text",
  id: Option[String] = Option.empty,
  label: Option[String] = Option.empty,
  description: Option[String] = Option.empty,
  value: String = "",
  placeholder: Option[String] = Option.empty,
  required: Boolean = false,
  disabled: Boolean = false,
  maxLength: Option[Int] = Option.empty,
  pattern: Option[String] = Option.empty,
  autocomplete: Option[String] = Option.empty,
  errors: Seq[String] = Seq.empty,
  dataAttributes: Map[String, String] = Map.empty
) extends FormField {
  DataAttributeName.requireValid(dataAttributes)
}

case class Button(
  name: String,
  label: String,
  buttonType: String = "submit",
  disabled: Boolean = false,
  dataAttributes: Map[String, String] = Map.empty
) extends FormField {
  DataAttributeName.requireValid(dataAttributes)
}

case class Textarea(
  name: String,
  id: Option[String] = Option.empty,
  label: Option[String] = Option.empty,
  description: Option[String] = Option.empty,
  value: String = "",
  placeholder: Option[String] = Option.empty,
  rows: Int = 4,
  required: Boolean = false,
  disabled: Boolean = false,
  maxLength: Option[Int] = Option.empty,
  errors: Seq[String] = Seq.empty,
  dataAttributes: Map[String, String] = Map.empty
) extends FormField {
  DataAttributeName.requireValid(dataAttributes)
}

case class Hidden(
  name: String,
  fields: Map[String, String]
) extends FormField

case class HelpToolTip(
  text: String,
  position: String = "top"
)
