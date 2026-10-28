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

package com.pastestack.surface.presenters

import com.pastestack.assetgraph.Jam
import com.pastestack.surface.rendering.ViewModelJson
import com.pastestack.surface.view_models.page.PageMeta
import com.pastestack.surface.view_models.components.Hero
import com.pastestack.surface.view_models.includes.{CSSInclude, JSInclude}
import com.pastestack.surface.view_model_factories.{PageMetaFactory, HeroFactory, IncludesFactory}

/**
 * Presenters are OPTIONAL.
 * 
 * == Simple sites ==
 * Just use ViewModels directly in templates:
 * {{{
 * @paste.surface.components.html.hero(Hero("main", showImage = true))
 * }}}
 * 
 * == Complex sites ==
 * Use presenters when you need:
 * - JSON API responses (same ViewModel, different serialization)
 * - HTML partials for AJAX
 * - Shared logic across multiple routes
 * - Testable presentation logic
 * 
 * Flow: Route -> Presenter -> ViewModel -> (Template OR JSON)
 */
trait PagePresenter[T] {
  def present(): T

  // The host supplies the JSON serializer; the library picks none. The
  // inherited no-arg default fails loudly rather than emitting case-class
  // toString or swallowing a presenter failure as "{}".
  @deprecated("Pass a ViewModelJson encoder", "next")
  def presentAsJson(): String =
    throw new UnsupportedOperationException("presentAsJson requires a ViewModelJson encoder")

  def presentAsJson(encoder: ViewModelJson[T]): String =
    encoder.encode(present())
}

/**
 * Standard page ViewModel containing all data needed to render a page.
 */
case class PageViewModel(
  pageMeta: PageMeta,
  cssIncludes: CSSInclude,
  jsIncludes: JSInclude,
  jam: Jam = Jam()
)

/**
 * Home page ViewModel with hero sections.
 */
case class HomePageViewModel(
  pageMeta: PageMeta,
  mainHero: Hero,
  sections: Seq[Hero],
  cssIncludes: CSSInclude,
  jsIncludes: JSInclude,
  jam: Jam = Jam()
)

/**
 * Example home page presenter.
 * 
 * Usage:
 * {{{
 * val presenter = new HomePagePresenter(siteConfig, assetConfig)
 * val viewModel = presenter.present()
 * // Pass viewModel to template
 * }}}
 */
class HomePagePresenter(
  siteConfig: PageMetaFactory.SiteConfig,
  assetConfig: IncludesFactory.AssetConfig,
  customModules: Seq[String] = Seq.empty
) extends PagePresenter[HomePageViewModel] {
  
  override def present(): HomePageViewModel = {
    HomePageViewModel(
      pageMeta = PageMetaFactory.forHomePage(siteConfig),
      mainHero = HeroFactory.forHomePage("main"),
      sections = Seq(
        HeroFactory.forSection("features", withImage = true),
        HeroFactory.forSection("about")
      ),
      cssIncludes = IncludesFactory.forDirectCSS(Seq("/css/main.css"), assetConfig.debugMode),
      jsIncludes = JSInclude(),
      jam = IncludesFactory.forPasteJam(customModules)
    )
  }
}
