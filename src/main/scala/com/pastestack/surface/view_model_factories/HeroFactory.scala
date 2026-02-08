package com.pastestack.surface.view_model_factories

import com.pastestack.surface.view_models.components.Hero

/**
 * Factory for creating Hero ViewModels.
 */
object HeroFactory {
  
  def forHomePage(name: String = "main"): Hero = Hero(
    name = name,
    showImage = true,
    stickyNavTarget = true,
    disableParallax = false
  )
  
  def forSection(name: String, withImage: Boolean = false): Hero = Hero(
    name = name,
    showImage = withImage,
    disableParallax = true
  )
  
  def forFullBleed(name: String): Hero = Hero(
    name = name,
    showImage = true,
    noContent = true,
    disableParallax = false
  )
}
