// Copyright (c) 2017–present Tomshley LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pastestack.surface.view_models.components

import play.twirl.api.Html

case class NavItem(
  label: String,
  targetId: String,
  cssClass: String = "",
  href: Option[String] = None
) {
  // The default link is the generated "#targetId" fragment, resolved by
  // exact id rather than as a selector — an item carrying an explicit
  // href never interpolates targetId, so the id rule binds only when the
  // fragment is actually generated.
  require(href != null, "NavItem href must not be null")
  if (href.isEmpty) GeneratedId.requireFragmentId("NavItem targetId", targetId)
}

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
) {
  // "#panelId" opens the panel and "#panelId-toggle" closes it — fragments
  // and aria-controls resolve by exact id, so the id must be a valid
  // fragment id, not a selector-safe identifier.
  GeneratedId.requireFragmentId("NavDisclosure panelId", panelId)
}

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
