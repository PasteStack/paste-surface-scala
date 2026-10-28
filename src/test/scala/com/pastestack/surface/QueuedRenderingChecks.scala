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
