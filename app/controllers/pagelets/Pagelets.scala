package controllers.pagelets

import javax.inject.Inject

import com.google.inject.Singleton
import play.api.Configuration
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc._
import service.{CarouselService, TeaserService, TextblockService}

import scala.concurrent.ExecutionContext

/**
  * Just the same pagelets as in HomeController only wrapped in classes, so the pagelets can be
  * used in different controllers without repetition. Used in HomeStreamController
  */

@Singleton
class CarouselPagelet @Inject()(carouselService: CarouselService)(
  implicit val ec: ExecutionContext) extends InjectedController with I18nSupport {
  import LangHelper._

  def carousel = Action.async { implicit request =>
    carouselService.carousel.map { teasers =>
      Ok(views.html.pagelets.carousel(teasers)).
        withCookies(Cookie("carouselCookie", "carouselValue"))
    }
  }
}

@Singleton
class HeaderPagelet @Inject()(conf: Configuration) extends InjectedController with I18nSupport {

  val supportedLanguages = conf.get[Seq[String]]("play.i18n.langs")
  val langForm = Form(single("language" -> nonEmptyText))

  def changeLanguage = Action { implicit request =>
    val target = request.headers.get(REFERER).getOrElse("/stream")

    langForm.bindFromRequest.fold(
      _ => BadRequest,
      lang =>
        if (supportedLanguages.contains(lang))
          Redirect(target).
            withLang(Lang(lang)).
            flashing(Flash(Map("success" -> Messages("language.change.flash", Messages(lang)))))
        else BadRequest
    )
  }

  def header = Action { implicit request =>
    Ok(views.html.pagelets.header())
  }
}

@Singleton
class TeaserPagelet @Inject()(teaserService: TeaserService)(
  implicit ec: ExecutionContext) extends InjectedController {
  import LangHelper._

  def teaser(typ: String)() = Action.async { implicit request =>
    teaserService.teaser(typ).map { teaser =>
      Ok(views.html.pagelets.teaser(teaser))
    }.recover { case _ =>
      Ok(views.html.pagelets.fallback(typ, "col-md-3"))
    }
  }
}

@Singleton
class TextPagelet @Inject()(textService: TextblockService)(
  implicit ec: ExecutionContext) extends InjectedController {
  import LangHelper._

  def text = Action.async { implicit request =>
    textService.text.map { text =>
      Ok(views.html.pagelets.text(text)).withCookies(Cookie("textCookie", "textValue"))
    }
  }
}

@Singleton
class FooterPagelet extends InjectedController with I18nSupport {

  def footer = Action { implicit request =>
    Ok(views.html.pagelets.footer())
  }
}


@Singleton
class FallbackPagelet extends InjectedController {

  def fallback(name: String)() = Action {
    Ok(views.html.pagelets.fallback(name, "col-md-12"))
  }
}


private object LangHelper {
  implicit def request2lang(implicit r: RequestHeader, messagesApi: MessagesApi): Lang = messagesApi.preferred(r).lang
}


