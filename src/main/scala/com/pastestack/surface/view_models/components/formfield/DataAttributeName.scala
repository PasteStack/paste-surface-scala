package com.pastestack.surface.view_models.components.formfield

object DataAttributeName {
  private val Name: scala.util.matching.Regex = "[a-z0-9][a-z0-9._-]*".r

  def isValid(name: String): Boolean =
    Option(name).exists(Name.matches)

  def requireValid(dataAttributes: Map[String, String]): Unit = {
    val invalid = Option(dataAttributes).fold(Iterable("null"))(_.keys.filterNot(isValid))
    require(invalid.isEmpty, s"data attribute names must match [a-z0-9][a-z0-9._-]*, got: ${invalid.mkString(",")}")
  }
}
