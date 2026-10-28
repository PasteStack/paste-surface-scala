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

package com.pastestack.surface.rendering

import play.twirl.api.Html

final case class IoReplyFragmentMarkup(
  markup: Html,
  js: Seq[String] = Seq.empty,
  css: Seq[String] = Seq.empty
) {
  require(Option(markup).exists(_.body != null), "IO reply fragment markup must be set")
  require(Option(js).exists(_.forall(_ != null)), "IO reply fragment markup js must be set without null entries")
  require(Option(css).exists(_.forall(_ != null)), "IO reply fragment markup css must be set without null entries")
}
