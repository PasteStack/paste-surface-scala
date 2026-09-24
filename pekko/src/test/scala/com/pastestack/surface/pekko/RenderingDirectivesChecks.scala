package com.pastestack.surface.pekko

import com.pastestack.surface.rendering.{Presentation, Rendering, ViewModelJson}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.complete
import org.apache.pekko.http.scaladsl.server.Route
import play.twirl.api.Html

import scala.concurrent.Await
import scala.concurrent.duration.*

object RenderingDirectivesChecks {
  private val timeout = 10.seconds

  private def varyValues(response: org.apache.pekko.http.scaladsl.model.HttpResponse): Seq[String] =
    response.headers.filter(_.is("vary")).map(_.value())

  private def bodyOf(response: org.apache.pekko.http.scaladsl.model.HttpResponse)(implicit system: ActorSystem): String = {
    implicit val ec: scala.concurrent.ExecutionContext = system.dispatcher
    Await.result(response.entity.toStrict(timeout).map(_.data.utf8String), timeout)
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("rendering-directives-checks")
    try {
      val serve = Route.toFunction(
        RenderingDirectives.rendering { r =>
          RenderingDirectives.completeRendered(
            StatusCodes.OK,
            Presentation[String](
              page = model => Html(s"<html><body>page:$model</body></html>"),
              ioReplyFragment = model => Html(s"<div>fragment:$model</div>"),
              json = Some((model: String) => s"""{"value":"$model"}""")
            ),
            "model",
            r
          )
        }
      )

      val fragmentResponse = Await.result(
        serve(HttpRequest(headers = List(RawHeader("X-Requested-With", "XMLHttpRequest")))), timeout)
      assert(fragmentResponse.status == StatusCodes.OK)
      assert(bodyOf(fragmentResponse) == "<div>fragment:model</div>")
      assert(varyValues(fragmentResponse) == Seq(Rendering.VaryHeader))

      val pageResponse = Await.result(serve(HttpRequest()), timeout)
      assert(pageResponse.status == StatusCodes.OK)
      assert(bodyOf(pageResponse) == "<html><body>page:model</body></html>")
      assert(varyValues(pageResponse) == Seq(Rendering.VaryHeader))

      val jsonResponse = Await.result(
        serve(HttpRequest(headers = List(RawHeader("Accept", "application/json")))), timeout)
      assert(jsonResponse.status == StatusCodes.OK)
      assert(jsonResponse.entity.contentType == ContentTypes.`application/json`)
      assert(bodyOf(jsonResponse) == """{"value":"model"}""")
      assert(varyValues(jsonResponse) == Seq(Rendering.VaryHeader))

      val noEncoder = Route.toFunction(
        RenderingDirectives.rendering { r =>
          RenderingDirectives.completeRendered(
            StatusCodes.OK,
            Presentation[String](
              page = model => Html(s"<html><body>page:$model</body></html>"),
              ioReplyFragment = model => Html(s"<div>fragment:$model</div>")
            ),
            "model",
            r
          )
        }
      )
      val notAcceptable = Await.result(
        noEncoder(HttpRequest(headers = List(RawHeader("Accept", "application/json")))), timeout)
      assert(notAcceptable.status == StatusCodes.NotAcceptable)
      assert(varyValues(notAcceptable) == Seq(Rendering.VaryHeader))

      val selected = Route.toFunction(
        RenderingDirectives.rendering { r => complete(s"${r}") }
      )
      assert(bodyOf(Await.result(
        selected(HttpRequest(headers = List(RawHeader("X-Requested-With", "XMLHttpRequest")))), timeout)) == "IoReplyFragment")
      assert(bodyOf(Await.result(
        selected(HttpRequest(headers = List(RawHeader("Accept", "application/json")))), timeout)) == "Json")
      assert(bodyOf(Await.result(selected(HttpRequest()), timeout)) == "Page")

      val customStatus = Route.toFunction(
        RenderingDirectives.rendering { r =>
          RenderingDirectives.completeRendered(
            StatusCodes.BadRequest,
            Presentation[String](
              page = model => Html(s"<html><body>page:$model</body></html>"),
              ioReplyFragment = model => Html(s"<div>fragment:$model</div>")
            ),
            "model",
            r
          )
        }
      )
      val badRequest = Await.result(
        customStatus(HttpRequest(headers = List(RawHeader("X-Requested-With", "XMLHttpRequest")))), timeout)
      assert(badRequest.status == StatusCodes.BadRequest)
      assert(bodyOf(badRequest) == "<div>fragment:model</div>")
      assert(varyValues(badRequest) == Seq(Rendering.VaryHeader))
    } finally {
      Await.result(system.terminate(), timeout)
    }
  }
}
