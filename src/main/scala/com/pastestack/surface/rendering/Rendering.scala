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
