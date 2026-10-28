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

package com.pastestack.surface.view_model_factories

import com.pastestack.surface.view_models.page.PageMeta

/**
 * Factory for creating PageMeta ViewModels from configuration.
 */
object PageMetaFactory {
  
  case class SiteConfig(
    siteName: String,
    domainName: String,
    themeColor: String = "",
    defaultDescription: String = "",
    defaultKeywords: String = ""
  )
  
  def create(
    config: SiteConfig,
    title: String,
    description: Option[String] = Option.empty,
    keywords: Option[String] = Option.empty,
    url: Option[String] = Option.empty,
    fbSiteType: Option[String] = Option.empty
  ): PageMeta = PageMeta(
    siteName = config.siteName,
    title = title,
    description = description.getOrElse(config.defaultDescription),
    keywords = keywords.getOrElse(config.defaultKeywords),
    url = url.getOrElse(config.domainName),
    themeColor = config.themeColor,
    fbSiteType = fbSiteType,
    domainName = config.domainName
  )
  
  def forHomePage(config: SiteConfig): PageMeta = create(
    config = config,
    title = config.siteName,
    fbSiteType = Some("website")
  )
  
  def forArticle(config: SiteConfig, title: String, description: String, url: String): PageMeta = create(
    config = config,
    title = title,
    description = Some(description),
    url = Some(url),
    fbSiteType = Some("article")
  )
}
