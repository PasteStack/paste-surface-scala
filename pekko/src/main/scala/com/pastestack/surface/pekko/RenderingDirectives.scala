package com.pastestack.surface.pekko

import com.pastestack.surface.rendering.{Presentation, Rendered, Rendering}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpResponse, StatusCode, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.{Directive1, Route}

import scala.concurrent.Future

trait RenderingDirectives {

  /** The rendering a request selects, per Rendering.select, from X-Requested-With and Accept. */
  def rendering: Directive1[Rendering] =
    (optionalHeaderValueByName("X-Requested-With") & optionalHeaderValueByName("Accept"))
      .tmap { case (xRequestedWith, accept) => Rendering.select(xRequestedWith, accept) }

  /** One outcome rendered for the selected rendering; always carries Vary: X-Requested-With, Accept.
    *
    * Renders synchronously: JAM includes reach the assetgraph (native calls and
    * file reads), so callers must run this off the request dispatcher —
    * `completeRendered` does so on the default-blocking-io dispatcher.
    */
  def renderedResponse[A](status: StatusCode, presentation: Presentation[A], model: A, rendering: Rendering): HttpResponse =
    presentation.render(model, rendering) match {
      case Some(rendered) =>
        HttpResponse(
          status = status,
          headers = scala.collection.immutable.Seq(RawHeader("Vary", Rendering.VaryHeader)),
          entity = HttpEntity(renderedContentType(rendered), rendered.body)
        )
      case None =>
        HttpResponse(
          status = StatusCodes.NotAcceptable,
          headers = scala.collection.immutable.Seq(RawHeader("Vary", Rendering.VaryHeader))
        )
    }

  /** `renderedResponse` completed on the blocking-io dispatcher — rendering
    * does native and filesystem work that must not occupy a request thread. */
  def completeRendered[A](status: StatusCode, presentation: Presentation[A], model: A, rendering: Rendering): Route =
    extractActorSystem { system =>
      val renderingIo = system.dispatchers.lookup("pekko.actor.default-blocking-io-dispatcher")
      complete(Future(renderedResponse(status, presentation, model, rendering))(renderingIo))
    }

  private def renderedContentType(rendered: Rendered): ContentType.NonBinary =
    rendered.contentType match {
      case Rendered.JsonContentType => ContentTypes.`application/json`
      case _ => ContentTypes.`text/html(UTF-8)`
    }
}

object RenderingDirectives extends RenderingDirectives
