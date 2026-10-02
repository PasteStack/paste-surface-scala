package com.pastestack.surface.view_models.components

import play.twirl.api.Html

case class NavItem(
  label: String,
  targetId: String,
  cssClass: String = "",
  href: Option[String] = None
)

/**
 * Fragment disclosure configuration for a SectionNav: the panel is the
 * `:target`, so a real link to `#panelId` opens it and a real link back to
 * the opener closes it. Disclosure state lives in the URL fragment, not in
 * script.
 *
 * `panelId` must be unique within the page — `href` fragments and
 * `aria-controls` resolve by id, so two navs sharing one would wire both
 * control pairs to the first matching panel. The opener's own id is derived
 * as `${panelId}-toggle`, so that id must be free as well.
 */
case class NavDisclosure(
  panelId: String,
  label: String = "Menu",
  footer: Option[Html] = None,
  closeLabel: String = "Close menu"
)

case class SectionNav(
  items: Seq[NavItem],
  sticky: Boolean = true,
  stickyMinWidth: Int = 0,
  scrollspy: Boolean = false,
  scrollspyMinWidth: Int = 0,
  cssClass: String = "",
  disclosure: Option[NavDisclosure] = None,
  scrollspyTouch: Boolean = false,
  ariaLabel: Option[String] = None
)
