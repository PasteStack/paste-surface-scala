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

// The id rules every component shares, one tier per way an id is used.
//
// `requireHtmlId` is HTML's own rule — nonempty, without whitespace —
// for an id emitted only as an element id and its for/id or IDREF
// partner, such as a form field's id. Nothing is prefixed to it, so a
// leading "#" is just another id character.
//
// `requireFragmentId` is that rule plus a refused leading "#", for an
// id that is a link fragment's target: one Surface writes after a "#"
// itself (a nav targetId, a disclosure panelId) or a section other
// markup links to (a hero name). A leading "#" there is a selector
// passed where an id is wanted, and it would render "##id". Fragments,
// aria-controls and :target resolve by exact id, never as selectors, so
// nothing more is required.
//
// `require` is the conservative rule for an id Surface interpolates
// into a generated "#id" selector the Elements parse with
// querySelectorAll — the media block ids a sequence derives. A name
// carrying selector metacharacters or a leading digit produces a
// selector that binds nothing, or the wrong element, without escaping,
// so it must be a plain identifier.
//
// Caller-supplied arbitrary selectors — ScrollScrub root/subject — are a
// different contract: they pass through verbatim and the DOM resolves
// or rejects them.
private[components] object GeneratedId {

  private val PlainId = "^[A-Za-z_][A-Za-z0-9_-]*$".r

  def require(owner: String, value: String): Unit =
    _root_.scala.Predef.require(
      Option(value).exists(id => PlainId.matches(id)),
      s"$owner must be a plain HTML id usable in a generated selector: " +
        "a letter or underscore, then letters, digits, underscores or hyphens"
    )

  def requireHtmlId(owner: String, value: String): Unit =
    _root_.scala.Predef.require(
      Option(value).exists(id => id.nonEmpty && !id.exists(_.isWhitespace)),
      s"$owner must be a nonempty HTML id without whitespace"
    )

  def requireFragmentId(owner: String, value: String): Unit =
    _root_.scala.Predef.require(
      Option(value).exists(id =>
        id.nonEmpty && !id.startsWith("#") && !id.exists(_.isWhitespace)
      ),
      s"$owner must be a nonempty HTML id without whitespace or a leading '#'"
    )
}
