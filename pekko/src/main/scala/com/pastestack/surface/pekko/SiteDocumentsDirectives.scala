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

package com.pastestack.surface.pekko

import com.pastestack.surface.documents.SiteDocuments
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

/** Routes for the rendered site documents (`spec/documents/site-documents.md`).
 *
 *  Both documents are pre-rendered strings — no blocking work — so they
 *  complete on the request dispatcher. The routes carry no response headers
 *  of their own: wrap them in the deployment's baseline security-header
 *  directive exactly as its page routes are wrapped, so a crawler-facing
 *  response cannot silently answer without the site's security posture.
 */
trait SiteDocumentsDirectives {

  def robotsRoute(documents: SiteDocuments): Route =
    path("robots.txt") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, documents.robotsTxt))
      }
    }

  def sitemapRoute(documents: SiteDocuments): Route =
    path("sitemap.xml") {
      get {
        complete(HttpEntity(ContentTypes.`text/xml(UTF-8)`, documents.sitemapXml))
      }
    }

  /** Both documents — the usual binding. A site wanting only one binds
   *  that route on its own. */
  def siteDocumentRoutes(documents: SiteDocuments): Route =
    robotsRoute(documents) ~ sitemapRoute(documents)
}

object SiteDocumentsDirectives extends SiteDocumentsDirectives
