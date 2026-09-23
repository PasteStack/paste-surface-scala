package com.pastestack.surface.rendering

enum Rendering {
  case Page, IoReplyFragment, Json
}

object Rendering {
  val VaryHeader: String = "X-Requested-With, Accept"

  def select(xRequestedWith: Option[String], accept: Option[String]): Rendering = {
    val mediaRanges = accept.toSeq
      .flatMap(_.split(','))
      .flatMap(_.split(';').map(_.trim).toList match {
        case range :: parameters if !parameters.exists(isZeroQuality) => Some(range.toLowerCase(java.util.Locale.ROOT))
        case _ => None
      })
    if (mediaRanges.contains("application/json") && !mediaRanges.contains("text/html")) {
      Rendering.Json
    } else if (xRequestedWith.exists(_.equalsIgnoreCase("XMLHttpRequest"))) {
      Rendering.IoReplyFragment
    } else {
      Rendering.Page
    }
  }

  private def isZeroQuality(parameter: String): Boolean = parameter.split("=", 2) match {
    case Array(name, value) => name.trim.equalsIgnoreCase("q") && value.trim.toDoubleOption.contains(0.0)
    case _ => false
  }
}
