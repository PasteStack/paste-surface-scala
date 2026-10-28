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

import com.pastestack.surface.view_models.components.formfield.Select
import play.twirl.api.Html
import scala.util.Try

object SelectRenderingChecks {
  private def render(field: Select): String =
    paste.surface.components.formfield.html.select(field)(
      Html("<optgroup label=\"Regions\"><option value=\"ct\" selected>Connecticut</option></optgroup>")
    ).body

  def main(args: Array[String]): Unit = {
    val implicitLabel = render(Select("region", label = Some("Region")))
    assert(implicitLabel.contains("<label>Region"))
    assert(implicitLabel.indexOf("</label>") > implicitLabel.indexOf("</select>"))
    assert(!implicitLabel.contains("id=\""))
    assert(!implicitLabel.contains("data-paste-select=\""))
    assert(implicitLabel.contains("<optgroup label=\"Regions\">"))
    assert(implicitLabel.contains("<option value=\"ct\" selected>Connecticut</option>"))
    val enhanced = render(Select("region", id = Some("shipping-region"), label = Some("Region"),
      description = Some("Shipping region"), errors = Seq("Choose a region"), required = true,
      autocomplete = Some("address-level1"), dataAttributes = Map("paste-select" -> "")))
    assert(enhanced.contains("for=\"shipping-region\""))
    assert(enhanced.contains("id=\"shipping-region\""))
    assert(enhanced.contains("aria-describedby=\"shipping-region-feedback\""))
    assert(enhanced.contains("id=\"shipping-region-feedback\""))
    assert(enhanced.contains("aria-invalid=\"true\""))
    assert(enhanced.contains("data-paste-select=\"\""))
    assert(enhanced.contains("data-paste-select-value aria-hidden=\"true\""))
    assert(!enhanced.contains("data-paste-select-ready"))
    assert(enhanced.contains("autocomplete=\"address-level1\""))
    val list = render(Select("regions", multiple = true, size = Some(4), disabled = true))
    assert(list.contains(" multiple ") && list.contains(" disabled ") && list.contains("size=\"4\""))
    val escaped = render(Select("region\" data-wrong=\"x", label = Some("<script>text</script>")))
    assert(escaped.contains("name=\"region&quot; data-wrong=&quot;x\""))
    assert(escaped.contains("&lt;script&gt;text&lt;/script&gt;"))
    assert(Try(Select("x", description = Some("help"))).isFailure)
    assert(Try(Select("x", id = Some("bad id"))).isFailure)
    assert(Try(Select("x", size = Some(0))).isFailure)
    assert(Try(Select("x", dataAttributes = Map("bad key" -> "x"))).isFailure)
    println("SelectRenderingChecks: PASS")
  }
}
