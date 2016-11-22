package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import models.{Image, Size, Teaser}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.{Action, Controller, RequestHeader, Result}

import scala.concurrent.Future
import scala.concurrent.duration._

class MockController @Inject()(system: ActorSystem) extends Controller {

  def teaser(typ: String) = MockAction(delay = 2000.millis) { implicit request =>
    Ok(Json.toJson(
      Teaser(
        translation.getOrElse(s"pagelet.${request2lang.language}", "...?") + typ,
        translation.getOrElse(s"teaser.text.${request2lang.language}", "...?"),
        Image("http://placehold.it/250x150",
          Size(250, 150),
          translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
    )).as(JSON)
  }

  def carousel = MockAction(delay = 800.millis) { implicit request =>
    Ok(
      Json.toJson(
        (1 to 3).map { count =>
          Teaser(
            translation.getOrElse(s"pagelet.${request2lang.language}", "...?") + count,
            translation.getOrElse(s"carousel.text.${request2lang.language}", "...?"),
            Image("http://placehold.it/1170x450",
              Size(250, 150),
              translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
        }
      )
    ).as(JSON)
  }

  def textblock = MockAction(delay = 1500.millis) { implicit request =>
    Ok(Json.toJson(
      Teaser(
        translation.getOrElse(s"pagelet.${request2lang.language}", "...?"),
        translation.getOrElse(s"textblock.text.${request2lang.language}", "...?"),
        Image("http://placehold.it/1170x50",
          Size(1170, 50),
          translation.getOrElse(s"placeholder.${request2lang.language}", "...?")))
    )).as(JSON)
  }

  def MockAction(delay: FiniteDuration)(f: RequestHeader => Result) = Action.async { request =>
    if (delay.lteq(0.millis)) Future.successful(f(request))
    else
      akka.pattern.after(delay, system.scheduler) {
        Future.successful(f(request))
      }
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
