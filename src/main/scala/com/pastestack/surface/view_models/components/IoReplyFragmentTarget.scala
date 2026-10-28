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

package com.pastestack.surface.view_models.components

final case class IoReplyFragmentTarget(
  name: String,
  cssClass: String = "",
  format: IoReplyFragmentFormat = IoReplyFragmentFormat.Html
) {
  require(format != null, "IoReplyFragmentTarget format must be set")
  require(IoReplyFragmentTarget.isValidName(name),
    s"IoReplyFragmentTarget name must be nonblank without whitespace, got: $name")
}

object IoReplyFragmentTarget {
  def isValidName(name: String): Boolean =
    Option(name).exists(n => n.nonEmpty && !n.exists(_.isWhitespace))
}
