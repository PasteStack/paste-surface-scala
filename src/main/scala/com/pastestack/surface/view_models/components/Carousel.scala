package com.pastestack.surface.view_models.components

case class CarouselSlide(
  content: String = "",
  cssClass: String = ""
)

final case class Carousel(
  name: String,
  autoCycleMs: Int = 0,
  showIndicators: Boolean = false,
  cssClass: String = "",
  variableWidth: Boolean = false,
  showCounter: Boolean = false,
  broadcastMoves: Boolean = false,
  animationEnabled: Boolean = false,
  controlsParentId: Option[String] = None,
  previousLabel: String = "Previous",
  nextLabel: String = "Next",
  pauseLabel: String = "Pause"
) {
  require(name != null, "Carousel name must not be null")
  require(autoCycleMs >= 0, "Carousel autoCycleMs must be nonnegative")
  require(controlsParentId != null, "Carousel controlsParentId must not be null")
  controlsParentId.foreach(GeneratedId.requireHtmlId("Carousel controlsParentId", _))
  Seq(previousLabel, nextLabel, pauseLabel).foreach { label =>
    require(Option(label).exists(_.trim.nonEmpty), "Carousel control labels must be nonblank")
  }
}
