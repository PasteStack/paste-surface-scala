package com.pastestack.surface.presenters

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
  
  // Override for JSON response
  def presentAsJson(): String = {
    import scala.util.Try
    Try {
      // Default: use case class toString, override for proper JSON
      present().toString
    }.getOrElse("{}")
  }
}

/**
 * Standard page ViewModel containing all data needed to render a page.
 */
case class PageViewModel(
  pageMeta: PageMeta,
  cssIncludes: CSSInclude,
  jsIncludes: JSInclude
)

/**
 * Home page ViewModel with hero sections.
 */
case class HomePageViewModel(
  pageMeta: PageMeta,
  mainHero: Hero,
  sections: Seq[Hero],
  cssIncludes: CSSInclude,
  jsIncludes: JSInclude
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
      jsIncludes = IncludesFactory.forPasteJam(assetConfig, customModules)
    )
  }
}
