package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import models.{Image, Size, Teaser}
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * A controller to simulate remote services which supply the data for the page. In the real world, the
  * data source might be some kind of micro-service or cms. To see how Pagelets react to corrupt data
  * or timeouts, you can change the delay for the individual actions or return broken json.
  */
class MockController @Inject()(system: ActorSystem) extends InjectedController {

  def request2lang(implicit r: RequestHeader) = messagesApi.preferred(r).lang

  def teaser(typ: String) = MockAction(delay = 60.millis) { implicit request =>
    Ok(Json.toJson(
      Teaser(
        translation.getOrElse(s"pagelet.${request2lang.language}", "...?") + typ,
        translation.getOrElse(s"teaser.text.${request2lang.language}", "...?"),
        Image(img(250, 150),
          Size(250, 150),
          translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
    )).as(JSON)
  }

  def carousel = MockAction(delay = 150.millis) { implicit request =>
    Ok(
      Json.toJson(
        (1 to 3).map { count =>
          Teaser(
            translation.getOrElse(s"pagelet.${request2lang.language}", "...?") + count,
            translation.getOrElse(s"carousel.text.${request2lang.language}", "...?"),
            Image(img(1170, 450),
              Size(250, 150),
              translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
        }
      )
    ).as(JSON)
  }

  def textblock = MockAction(delay = 90.millis) { implicit request =>
    Ok(Json.toJson(
      Teaser(
        translation.getOrElse(s"pagelet.${request2lang.language}", "...?"),
        translation.getOrElse(s"textblock.text.${request2lang.language}", "...?"),
        Image(img(1170, 50),
          Size(1170, 50),
          translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
    )).as(JSON)
  }



  def img(w:Int, h:Int)  = s"https://via.placeholder.com/${w}x$h?text=${w}x$h"

  def MockAction(delay: FiniteDuration)(f: RequestHeader => Result) = Action.async { request =>
    if (delay.lteq(0.millis)) Future.successful(f(request))
    else
      akka.pattern.after(delay, system.scheduler) {
        Future.successful(f(request))
      }(controllerComponents.executionContext)
  }

  val translation = Map[String, String](
    "pagelet.en" -> "Pagelet",
    "pagelet.de" -> "Pagelet",
    "placeholder.en" -> "Placeholder",
    "placeholder.de" -> "Platzhalter",
    "carousel.text.en" -> "Of be talent me answer do relied.",
    "carousel.text.de" -> "Fischen wollten gut gar stunden.",
    "teaser.text.en" -> "Post no so what deal evil rent by real in. She offices for highest and replied one venture ready spite solid.",
    "teaser.text.de" -> "Gefällt ähnlich diesmal bis es pfeifen richten um Sonntag. Dann las dahin statt. Anzug holen damit bis still.",
    "textblock.text.en" -> "On am we offices expense thought. Its hence ten smile age means. Seven chief sight far point any. Of so high into easy. Dashwoods eagerness oh extensive as discourse sportsman frankness. Husbands see disposed surprise likewise humoured yet pleasure. Fifteen no inquiry cordial so resolve garrets as. Impression was estimating surrounded solicitude indulgence son shy. Is post each that just leaf no. He connection interested so we an sympathize advantages. To said is it shed want do. Occasional middletons everything so to. Have spot part for his quit may. Enable it is square my an regard. Often merit stuff first oh up hills as he.",
    "textblock.text.de" -> "Gelernte gespielt sa er fröhlich zu sprechen. Zu dabei faden ja nötig. Es em dazwischen vorsichtig unsicherer sei. Gang ja im etwa da ja en plötzlich duftenden unterwegs es schnellen. So mageren es ja zuhörte gerufen sondern nachdem spuckte. Bas gründlich fur ausdenken vom schwachen ausblasen kam. Menschen ja Mädchen zu da. Und da verlogen brannten gegangen in Talseite gemessen. Erst leer floh auf habt wohl. Gegenden vor brotlose mitreden ratloses prächtig. Wer was stickig schritt uberall sei Zwiebel Paprika schones richtig. Das so launisch kurioses Zeug ziemlich da. Zum gegriffen wir lag neugierig Filzhüte. Munter soviel em je keinem. Ein lag Eleganz erzählt nur lockere Fenster art gewohnt."
  )
}
