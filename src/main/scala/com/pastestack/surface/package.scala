package com.pastestack

/**
 * PasteStack Surface - MVP-inspired templating layer
 * 
 * Package structure:
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
  
  type NavDisclosure = view_models.components.NavDisclosure
  val NavDisclosure = view_models.components.NavDisclosure

  type SectionNav = view_models.components.SectionNav
  val SectionNav = view_models.components.SectionNav

  type Marquee = view_models.components.Marquee
  val Marquee = view_models.components.Marquee
  
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

  type Form = view_models.components.formfield.Form
  val Form = view_models.components.formfield.Form

  type FormMethod = view_models.components.formfield.FormMethod
  val FormMethod = view_models.components.formfield.FormMethod

  type FormEnctype = view_models.components.formfield.FormEnctype
  val FormEnctype = view_models.components.formfield.FormEnctype

  val DataAttributeName = view_models.components.formfield.DataAttributeName

  type IoReplyFragmentTarget = view_models.components.IoReplyFragmentTarget
  val IoReplyFragmentTarget = view_models.components.IoReplyFragmentTarget

  // Components: declarative media (paste.ui.* element declarations)
  type ScrollScrub = view_models.components.ScrollScrub
  val ScrollScrub = view_models.components.ScrollScrub

  type ScrubMode = view_models.components.ScrubMode
  val ScrubMode = view_models.components.ScrubMode

  type ImageRepresentation = view_models.components.ImageRepresentation
  val ImageRepresentation = view_models.components.ImageRepresentation

  type ImageSequenceFrame = view_models.components.ImageSequenceFrame
  val ImageSequenceFrame = view_models.components.ImageSequenceFrame

  type ImageSequenceCatalog = view_models.components.ImageSequenceCatalog
  val ImageSequenceCatalog = view_models.components.ImageSequenceCatalog

  type ImageSequenceIntro = view_models.components.ImageSequenceIntro
  val ImageSequenceIntro = view_models.components.ImageSequenceIntro

  type IntroEasing = view_models.components.IntroEasing
  val IntroEasing = view_models.components.IntroEasing

  type ImageSequence = view_models.components.ImageSequence
  val ImageSequence = view_models.components.ImageSequence

  type ProgressiveImage = view_models.components.ProgressiveImage
  val ProgressiveImage = view_models.components.ProgressiveImage

  type MediaId = view_models.components.MediaId
  val MediaId = view_models.components.MediaId

  // Includes (JAM module names only; the paste-assetgraph page renders markup)
  type CSSInclude = view_models.includes.CSSInclude
  val CSSInclude = view_models.includes.CSSInclude
  
  type JSInclude = view_models.includes.JSInclude
  val JSInclude = view_models.includes.JSInclude
  
  // === View Model Factories ===
  val PageMetaFactory = view_model_factories.PageMetaFactory
  val HeroFactory = view_model_factories.HeroFactory
  val IncludesFactory = view_model_factories.IncludesFactory
  type AdaptiveImageSequenceManifest = view_model_factories.AdaptiveImageSequenceManifest
  val AdaptiveImageSequenceManifest = view_model_factories.AdaptiveImageSequenceManifest
  
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

  // === Rendering (Page/IoReplyFragment/Json from one view model) ===
  type Rendering = rendering.Rendering
  val Rendering = rendering.Rendering

  type ViewModelJson[A] = rendering.ViewModelJson[A]

  type Rendered = rendering.Rendered
  val Rendered = rendering.Rendered

  type Presentation[A] = rendering.Presentation[A]
  val Presentation = rendering.Presentation

  type IoReplyFragmentEnvelopeErrorType = rendering.IoReplyFragmentEnvelopeErrorType
  val IoReplyFragmentEnvelopeErrorType = rendering.IoReplyFragmentEnvelopeErrorType

  type IoReplyFragmentEnvelopeError = rendering.IoReplyFragmentEnvelopeError
  val IoReplyFragmentEnvelopeError = rendering.IoReplyFragmentEnvelopeError

  type IoReplyFragmentMarkup = rendering.IoReplyFragmentMarkup
  val IoReplyFragmentMarkup = rendering.IoReplyFragmentMarkup

  val IoReplyFragmentEnvelope = rendering.IoReplyFragmentEnvelope

  // === Config (load from resources) ===
  type SurfaceConfig = config.SurfaceConfig
  val SurfaceConfig = config.SurfaceConfig
}
