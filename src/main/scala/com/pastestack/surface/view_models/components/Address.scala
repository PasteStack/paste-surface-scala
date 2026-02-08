package com.pastestack.surface.view_models.components

case class Address(
  street1: String,
  locality: String,
  region: String,
  postalCode: String,
  countryCode: String,
  street2: Option[String] = Option.empty,
  regionTitle: Option[String] = Option.empty
)
