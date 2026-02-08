package com.pastestack.surface.view_models.components

case class NavItem(
  label: String,
  targetId: String,
  cssClass: String = ""
)

case class SectionNav(
  items: Seq[NavItem],
  sticky: Boolean = true,
  cssClass: String = ""
)
