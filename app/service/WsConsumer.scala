package service

import com.google.inject.Inject
import play.api.Logger
import play.api.http.{CookiesConfiguration, HeaderNames}
import play.api.i18n.Lang
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Cookie, CookieHeaderEncoding, Cookies, DefaultCookieHeaderEncoding}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * Consume Json data from a remote service and translate the Json to case classes.
  *
  * Note the timeout parameter which states that requests which take longer then
  * 2 seconds are considered a failure.
  */
trait WsConsumer {
  def fetch[T](url: String, timeout: FiniteDuration = 2.seconds)(implicit ec:ExecutionContext, r: Reads[T], lang: Lang): Future[T]
}

class WsConsumerImpl @Inject()(ws: WSClient) extends WsConsumer with CookieHeaderEncoding {
  val log = Logger("WsConsumer")

  override protected def config = CookiesConfiguration()

  override def fetch[T](url: String, timeout: FiniteDuration = 2.seconds)(implicit ec:ExecutionContext, r: Reads[T], lang: Lang) =
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
      val encoded = encodeSetCookieHeader(cookies.toSeq)
      requestHolder.withHttpHeaders(HeaderNames.COOKIE -> encoded)
    }
  }

}
