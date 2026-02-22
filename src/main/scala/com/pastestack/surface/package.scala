package com.pastestack

/**
 * PasteStack Surface - MVP-inspired templating layer
 * 
 * Structure inspired by srv/up/wellness:
 * - view_models: Data classes for templates
 * - view_model_factories: Create ViewModels from domain data
 * - presenters: Orchestrate complex pages
 * 
 * Flow: Presenter -> Service -> ViewModelFactory -> ViewModel -> Template
 */
package object surface {
  // === View Models ===
  
  // Page
  type PageMeta = view_models.page.PageMeta
  val PageMeta = view_models.page.PageMeta
  
  // Components
  type Hero = view_models.components.Hero
  val Hero = view_models.components.Hero
  
  type Address = view_models.components.Address
  val Address = view_models.components.Address
  
  type NavItem = view_models.components.NavItem
  val NavItem = view_models.components.NavItem
  
  type SectionNav = view_models.components.SectionNav
  val SectionNav = view_models.components.SectionNav
  
  type Logo = view_models.components.Logo
  val Logo = view_models.components.Logo
  
  type Carousel = view_models.components.Carousel
  val Carousel = view_models.components.Carousel
  
  type CarouselSlide = view_models.components.CarouselSlide
  val CarouselSlide = view_models.components.CarouselSlide
  
  // Components: Form Fields
  type Input = view_models.components.formfield.Input
  val Input = view_models.components.formfield.Input
  
  type Button = view_models.components.formfield.Button
  val Button = view_models.components.formfield.Button
  
  type Textarea = view_models.components.formfield.Textarea
  val Textarea = view_models.components.formfield.Textarea
  
  type Hidden = view_models.components.formfield.Hidden
  val Hidden = view_models.components.formfield.Hidden
  
  // Includes (aggregates JAM calls for page rendering)
  type CSSInclude = view_models.includes.CSSInclude
  val CSSInclude = view_models.includes.CSSInclude
  
  type JSInclude = view_models.includes.JSInclude
  val JSInclude = view_models.includes.JSInclude
  
  // === View Model Factories ===
  val PageMetaFactory = view_model_factories.PageMetaFactory
  val HeroFactory = view_model_factories.HeroFactory
  val IncludesFactory = view_model_factories.IncludesFactory
  
  // Factory config types
  type SiteConfig = view_model_factories.PageMetaFactory.SiteConfig
  val SiteConfig = view_model_factories.PageMetaFactory.SiteConfig
  
  type AssetConfig = view_model_factories.IncludesFactory.AssetConfig
  val AssetConfig = view_model_factories.IncludesFactory.AssetConfig
  
  // === Presenters (OPTIONAL - for complex sites needing JSON/HTML) ===
  type PageViewModel = presenters.PageViewModel
  val PageViewModel = presenters.PageViewModel
  
  type HomePageViewModel = presenters.HomePageViewModel
  val HomePageViewModel = presenters.HomePageViewModel
  
  type HomePagePresenter = presenters.HomePagePresenter
  
  // === Config (load from resources) ===
  type SurfaceConfig = config.SurfaceConfig
  val SurfaceConfig = config.SurfaceConfig
}
