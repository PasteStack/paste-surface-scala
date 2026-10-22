package com.pastestack.surface.view_models.components.formfield

import com.pastestack.surface.view_models.components.GeneratedId

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
  // A label pairs with its control through for/id when an id is
  // declared; an empty or whitespace id would leave it bound to nothing.
  // The id is never written after a "#", so HTML's own id rule applies.
  require(id != null, "Input id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Input id", _))
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
  // A label pairs with its control through for/id when an id is
  // declared; an empty or whitespace id would leave it bound to nothing.
  // The id is never written after a "#", so HTML's own id rule applies.
  require(id != null, "Textarea id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Textarea id", _))
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
