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

final case class Presentation[A](
  page: A => Html,
  ioReplyFragment: A => Html,
  json: Option[ViewModelJson[A]] = None
) {
  def render(model: A, rendering: Rendering): Option[Rendered] = rendering match {
    case Rendering.Page =>
      Some(Rendered(Rendered.HtmlContentType, page(model).body))
    case Rendering.IoReplyFragment =>
      Some(Rendered(Rendered.HtmlContentType, ioReplyFragment(model).body))
    case Rendering.Json =>
      json.map(encoder => Rendered(Rendered.JsonContentType, encoder.encode(model)))
  }
}
