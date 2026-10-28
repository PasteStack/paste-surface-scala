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

import com.pastestack.surface.view_models.components.GeneratedId

final case class Form(
  action: String,
  method: FormMethod = FormMethod.Post,
  enctype: FormEnctype = FormEnctype.UrlEncoded,
  id: Option[String] = Option.empty,
  submission: FormSubmission = FormSubmission.Native,
  errors: Seq[String] = Seq.empty,
  cssClass: String = "",
  dataAttributes: Map[String, String] = Map.empty
) {
  require(Option(action).exists(_.trim.nonEmpty), "Form action must be nonblank")
  require(method != null, "Form method must be set")
  require(enctype != null, "Form enctype must be set")
  require(submission != null, "Form submission must be set")
  require(submission != FormSubmission.Apply || method == FormMethod.Post, "Apply submission requires POST")
  require(id != null, "Form id must not be null")
  id.foreach(GeneratedId.requireHtmlId("Form id", _))
  DataAttributeName.requireValid(dataAttributes)
}
