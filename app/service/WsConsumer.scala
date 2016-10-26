package service

import com.google.inject.Inject
import play.api.Logger
import play.api.http.HeaderNames
import play.api.i18n.Lang
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Cookie, Cookies}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait WsConsumer {
  def fetch[T](url: String, timeout: FiniteDuration = 500.millis)(implicit r: Reads[T], lang: Lang): Future[T]
}

class WsConsumerImpl @Inject()(ws: WSClient) extends WsConsumer {
  val log = Logger("WsConsumer")

  override def fetch[T](url: String, timeout: FiniteDuration = 500.millis)(implicit r: Reads[T], lang: Lang) =
    ws.url(url).
      withCookies(Cookie("PLAY_LANG", lang.language)).
      withRequestTimeout(timeout).
      get().flatMap { result =>
      Json.fromJson(result.json) match {
        case JsSuccess(s, _) =>
          Future.successful(s)
        case JsError(e) =>
          val errors = JsError.toJson(e).fields.foldLeft("")((acc, next) => acc + next + "\n")
          Future.failed(new RuntimeException(s"Failed to unmarshal:\n $errors"))
      }
    }.recoverWith {
      case NonFatal(t) =>
        log.warn(s"Failed to fetch '$url'")
        Future.failed(new RuntimeException(t))
    }

  implicit class WithCookiesOps(requestHolder: WSRequest) {
    def withCookies(cookies: Cookie*): WSRequest = {
      val encoded = Cookies.encodeSetCookieHeader(cookies.toSeq)
      requestHolder.withHeaders(HeaderNames.COOKIE -> encoded)
    }
  }

}