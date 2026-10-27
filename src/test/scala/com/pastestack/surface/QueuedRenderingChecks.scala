package com.pastestack.surface

import com.pastestack.surface.view_models.components.*
import scala.util.Try

object QueuedRenderingChecks {
  def main(args: Array[String]): Unit = {
    val catalog = ImageSequenceCatalog(Seq(ImageSequenceFrame("/frame.webp", 100, 100)))
    def render(playback: SequencePlayback): String =
      paste.surface.components.html.imagesequence(
        ImageSequence(catalog, playback = playback), MediaId("queue-example")
      ).body

    assert(!render(SequencePlayback.Direct).contains("data-sequence-playback"))
    Seq(1, 30, 40, 120).foreach { fps =>
      val html = render(SequencePlayback.Queued(fps))
      assert(html.contains("data-sequence-playback=\"queued\""))
      assert(html.contains(s"data-sequence-playback-fps=\"$fps\""))
    }
    assert(SequencePlayback.Queued() == SequencePlayback.Queued(30))
    Seq(-1, 0, 121).foreach { fps =>
      assert(Try(ImageSequence(catalog, playback = SequencePlayback.Queued(fps))).isFailure)
    }
    assert(Try(ImageSequence(catalog, playback = null)).isFailure)
  }
}
