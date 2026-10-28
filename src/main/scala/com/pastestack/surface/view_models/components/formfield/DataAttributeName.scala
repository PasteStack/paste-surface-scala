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

package com.pastestack.surface.view_models.components.formfield

object DataAttributeName {
  private val Name: scala.util.matching.Regex = "[a-z0-9][a-z0-9._-]*".r

  def isValid(name: String): Boolean =
    Option(name).exists(Name.matches)

  def requireValid(dataAttributes: Map[String, String]): Unit = {
    val invalid = Option(dataAttributes).fold(Iterable("null"))(_.keys.filterNot(isValid))
    require(invalid.isEmpty, s"data attribute names must match [a-z0-9][a-z0-9._-]*, got: ${invalid.mkString(",")}")
  }
}
