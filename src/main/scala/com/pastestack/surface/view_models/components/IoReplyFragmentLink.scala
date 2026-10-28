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

final case class IoReplyFragmentLink(
  href: String,
  target: String,
  cssClass: String = ""
) {
  require(Option(href).exists(_.trim.nonEmpty), "IoReplyFragmentLink href must be nonblank")
  require(IoReplyFragmentTarget.isValidName(target), "IoReplyFragmentLink target must be a valid target name")
}
