package com.pastestack.surface.view_models.components

case class NavItem(
  label: String,
  targetId: String,
  cssClass: String = ""
)

case class SectionNav(
  items: Seq[NavItem],
  sticky: Boolean = true,
  stickyMinWidth: Int = 0,
  scrollspy: Boolean = false,
  scrollspyMinWidth: Int = 0,
  cssClass: String = ""
)
