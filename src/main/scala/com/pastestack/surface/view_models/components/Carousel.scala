package com.pastestack.surface.view_models.components

case class CarouselSlide(
  content: String = "",
  cssClass: String = ""
)

case class Carousel(
  name: String,
  autoCycleMs: Int = 0,
  showIndicators: Boolean = false,
  cssClass: String = ""
)
