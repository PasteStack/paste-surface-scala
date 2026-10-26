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
  // A label uses for/id when declared and wraps the control otherwise.
  // Help and errors require a caller-owned id for their association.
  // The id is never written after a "#", so HTML's own id rule applies.
  require(id != null, "Input id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Input id", _))
  require(id.nonEmpty || (description.isEmpty && errors.isEmpty),
    "Input descriptions and errors require a caller-owned id")
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
  // A label uses for/id when declared and wraps the control otherwise.
  // Help and errors require a caller-owned id for their association.
  // The id is never written after a "#", so HTML's own id rule applies.
  require(id != null, "Textarea id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Textarea id", _))
  require(id.nonEmpty || (description.isEmpty && errors.isEmpty),
    "Textarea descriptions and errors require a caller-owned id")
  DataAttributeName.requireValid(dataAttributes)
}

/** Options and optgroups are authored in the template's content slot. */
case class Select(
  name: String,
  id: Option[String] = Option.empty,
  label: Option[String] = Option.empty,
  description: Option[String] = Option.empty,
  required: Boolean = false,
  disabled: Boolean = false,
  multiple: Boolean = false,
  size: Option[Int] = Option.empty,
  autocomplete: Option[String] = Option.empty,
  errors: Seq[String] = Seq.empty,
  dataAttributes: Map[String, String] = Map.empty
) extends FormField {
  require(id != null, "Select id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Select id", _))
  require(id.nonEmpty || (description.isEmpty && errors.isEmpty),
    "Select descriptions and errors require a caller-owned id")
  require(size.forall(_ > 0), "Select size must be positive")
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
