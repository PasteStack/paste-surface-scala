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

import com.pastestack.surface.view_models.components.{Carousel, LazyContent}
import com.pastestack.surface.view_models.components.formfield.{Form, FormMethod, FormSubmission}
import play.twirl.api.Html
import scala.util.Try

object RestorationRenderingChecks {
  def main(args: Array[String]): Unit = {
    val native = paste.surface.components.formfield.html.form(Form("/save"))(Html("fields")).body
    val apply = paste.surface.components.formfield.html.form(Form("/save", submission = FormSubmission.Apply))(Html("fields")).body
    val html = paste.surface.components.formfield.html.form(Form("/save", submission = FormSubmission.Html))(Html("fields")).body
    assert(native.contains("class=\"paste-ui-form\""))
    assert(apply.contains("class=\"paste-ui-form paste-ui-apply\""))
    assert(html.contains("class=\"paste-ui-form paste-ui-formhtml\""))
    assert(!Seq(native, apply, html).exists(_.contains("data-paste-form")))
    assert(Try(Form("/save", method = FormMethod.Get, submission = FormSubmission.Apply)).isFailure)
    assert(Try(Form("/save", submission = null)).isFailure)

    val carousel = paste.surface.components.html.carousel(Carousel("gallery", autoCycleMs = 5000,
      showIndicators = true, variableWidth = true, showCounter = true, broadcastMoves = true, animationEnabled = true,
      controlsParentId = Some("controls"), nextLabel = "Next & more"))(Html("<article>Slide</article>")).body
    assert(carousel.contains("data-paste-carousel-autocycle=\"5000\""))
    assert(carousel.contains("data-paste-carousel-slide-width=\"variable\""))
    assert(carousel.contains("data-paste-carousel-animation-enabled"))
    assert(carousel.contains("data-paste-carousel-controlsparent=\"controls\""))
    assert(carousel.contains("data-paste-carousel-next-label=\"Next &amp; more\""))
    assert(carousel.contains("class=\"paste-ui-carousel-wrap\""))
    assert(carousel.contains("class=\"paste-ui-carousel-items\""))
    assert(Try(Carousel("x", autoCycleMs = -1)).isFailure)
    assert(Try(Carousel("x", controlsParentId = Some("bad id"))).isFailure)

    val lazyContent = paste.surface.components.html.lazyContent(LazyContent("/fragment?x=1&y=2", Some("lazy"), "panel"))(Html("<p>Fallback</p>")).body
    assert(lazyContent.contains("data-lazy-load=\"/fragment?x=1&amp;y=2\""))
    assert(lazyContent.contains(" id=\"lazy\""))
    assert(lazyContent.contains(" class=\"panel\""))
    assert(lazyContent.contains("<p>Fallback</p>"))
    assert(Try(LazyContent(" ")).isFailure)
    assert(Try(LazyContent("/x", Some("bad id"))).isFailure)
    println("RestorationRenderingChecks passed")
  }
}
