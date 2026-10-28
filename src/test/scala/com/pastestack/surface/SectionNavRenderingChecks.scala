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

package com.pastestack.surface

import com.pastestack.surface.view_models.components.{NavDisclosure, NavItem, SectionNav}
import play.twirl.api.Html

object SectionNavRenderingChecks {
  private val items = Seq(NavItem("Overview", "overview"), NavItem("Details", "details"))

  private val expectedLegacy =
    """
    |
    |<nav class="paste-ui-section-nav" data-paste-sticky-nav>
    |    <ul class="paste-ui-section-nav-items">
    |        
    |        <li class="paste-ui-section-nav-item">
    |            <a class="paste-ui-section-nav-link" href="#overview">Overview</a>
    |        </li>
    |        
    |        <li class="paste-ui-section-nav-item">
    |            <a class="paste-ui-section-nav-link" href="#details">Details</a>
    |        </li>
    |        
    |    </ul>
    |</nav>
    |""".stripMargin

  private def render(nav: SectionNav): String =
    paste.surface.components.html.sectionNav(nav).body

  private def count(haystack: String, needle: String): Int =
    haystack.sliding(needle.length).count(_ == needle)

  def main(args: Array[String]): Unit = {
    val legacy = render(SectionNav(items))
    assert(legacy == expectedLegacy, s"legacy render mismatch: expected=${expectedLegacy.replace("\n", "\\n")} actual=${legacy.replace("\n", "\\n")}")

    val disclosed = render(
      SectionNav(
        items,
        sticky = false,
        scrollspy = true,
        scrollspyTouch = true,
        ariaLabel = Some("Site sections"),
        disclosure = Some(NavDisclosure("site-sections", footer = Some(Html("<p>Contact</p>"))))
      )
    )
    assert(disclosed.contains("<nav class=\"paste-ui-section-nav paste-ui-section-nav-disclosure\""))
    assert(disclosed.contains("data-paste-scrollspy data-spy-touch aria-label=\"Site sections\""))
    assert(disclosed.contains("<div id=\"site-sections\" class=\"paste-ui-section-nav-panel\" tabindex=\"-1\">"))
    assert(disclosed.contains("<a id=\"site-sections-toggle\" class=\"paste-ui-section-nav-toggle paste-ui-section-nav-open\" href=\"#site-sections\" aria-controls=\"site-sections\">"))
    assert(disclosed.contains("<a class=\"paste-ui-section-nav-toggle paste-ui-section-nav-close\" href=\"#site-sections-toggle\" aria-controls=\"site-sections\">"))
    val panelIndex = disclosed.indexOf("id=\"site-sections\"")
    val openIndex = disclosed.indexOf("id=\"site-sections-toggle\"")
    val closeIndex = disclosed.indexOf("paste-ui-section-nav-close")
    assert(panelIndex >= 0 && openIndex > panelIndex && closeIndex > openIndex,
      "disclosure order must be panel, then opener, then closer")
    assert(disclosed.contains("<span class=\"paste-ui-visually-hidden\">Menu</span>"))
    assert(disclosed.contains("<span class=\"paste-ui-visually-hidden\">Close menu</span>"))
    assert(disclosed.contains("<p>Contact</p>"))
    assert(count(disclosed, "href=\"#overview\"") == 1, "each section link renders exactly once")
    assert(count(disclosed, "href=\"#details\"") == 1, "each section link renders exactly once")
    assert(!disclosed.contains("popover"))
    assert(!disclosed.contains("aria-expanded"))
    assert(!disclosed.contains("<script"))
    assert(!disclosed.contains("onclick"))

    val customClose = render(
      SectionNav(
        items,
        sticky = false,
        disclosure = Some(NavDisclosure("other-sections", label = "Open <nav>", closeLabel = "Close & exit"))
      )
    )
    assert(customClose.contains("id=\"other-sections-toggle\""))
    assert(customClose.contains("href=\"#other-sections-toggle\""))
    assert(customClose.contains("Open &lt;nav&gt;"))
    assert(customClose.contains("Close &amp; exit"))
    assert(!customClose.contains("popover"))

    val touchWithoutScrollspy = render(
      SectionNav(
        items,
        sticky = false,
        scrollspy = false,
        scrollspyTouch = true,
        disclosure = Some(NavDisclosure("quiet-sections"))
      )
    )
    assert(!touchWithoutScrollspy.contains("data-paste-scrollspy"))
    assert(!touchWithoutScrollspy.contains("data-spy-touch"))

    val linked = render(
      SectionNav(
        Seq(NavItem("Approach", "approach", href = Some("/#approach")), NavItem("Home", "", href = Some("/"))),
        sticky = false
      )
    )
    assert(linked.contains("href=\"/#approach\""), "explicit href is emitted verbatim")
    assert(linked.contains("href=\"/\""), "explicit href is emitted verbatim")
    assert(!linked.contains("href=\"#approach\""), "explicit href suppresses the generated fragment")
  }
}
