package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.pagelets.TwirlConversions._
import org.splink.pagelets._
import play.api.{Configuration, Environment}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import service.{CarouselService, TeaserService, TextblockService}
import views.html.{error, wrapper}

@Singleton
class HomeController @Inject()(pagelets: Pagelets,
                               teaserService: TeaserService,
                               carouselService: CarouselService,
                               textblockService: TextblockService)(
                                implicit m: Materializer,
                                e: Environment,
                                conf: Configuration,
                                val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val log = play.api.Logger(getClass).logger

  import pagelets._

  def tree(r: RequestHeader) = {

    //make the request implicitly available to the sections combiner template
    implicit val request: RequestHeader = r

    val tree = Tree('root, Seq(
      Leaf('header, header _),
      Tree('content, Seq(
        Leaf('carousel, carousel _).withFallback(fallback("Carousel") _),
        Leaf('text, text _).withFallback(fallback("Text") _),
        Tree('teasers, Seq(
          Leaf('teaserA, teaser("A") _),
          Leaf('teaserB, teaser("B") _),
          Leaf('teaserC, teaser("C") _),
          Leaf('teaserD, teaser("D") _)
        ), results => combine(results)(views.html.pagelets.teasers.apply))
      )),
      Leaf('footer, footer _)
    ), results => combine(results)(views.html.pagelets.sections.apply))

    request2lang.language match {
      case "de" => tree.skip('text)
      case _ => tree
    }
  }

  val mainTemplate = wrapper(routes.HomeController.resourceFor) _
  val errorTemplate = error(_)

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def index = PageAction(errorTemplate)(Messages("title"), tree) { (request, page) =>
    log.info("\n" + visualize(tree(request)))
    mainTemplate(page)
  }

  def pagelet(id: Symbol) = PageletAction(errorTemplate)(tree, id) { (request, page) =>
    mainTemplate(page)
  }

  val supportedLanguages = conf.getStringSeq("play.i18n.langs").get
  val langForm = Form(single("language" -> nonEmptyText))

  def changeLanguage = Action { implicit request =>
    langForm.bindFromRequest.fold(
      errors => BadRequest,
      lang =>
        if (supportedLanguages.contains(lang))
          Redirect(routes.HomeController.index()).
            withCookies(Cookie(messagesApi.langCookieName, lang)).
            flashing(Flash(Map("success" -> Messages("language.change.flash", Messages(lang)))))
        else BadRequest
    )
  }

  def header = Action { implicit request =>
    Ok(views.html.pagelets.header()).
      withJavascript(
        Javascript("lib/bootstrap/js/dropdown.min.js"),
        Javascript("lib/bootstrap/js/alert.min.js")
      ).withMetaTags(
      MetaTag("description", Messages("metaTags.description"))
    )
  }

  def carousel = Action.async { implicit request =>
    carouselService.carousel.map { teasers =>
      Ok(views.html.pagelets.carousel(teasers)).
        withJavascript(
          Javascript("lib/bootstrap/js/transition.min.js"),
          Javascript("lib/bootstrap/js/carousel.min.js"))
    }
  }

  def teaser(typ: String)() = Action.async { implicit request =>
    teaserService.teaser(typ).map { teaser =>
      Ok(views.html.pagelets.teaser(teaser))
    }.recover { case _ =>
      Ok(views.html.pagelets.fallback(typ, "col-md-3"))
    }
  }

  def text = Action.async { implicit request =>
    textblockService.text.map { text =>
      Ok(views.html.pagelets.text(text))
    }
  }

  def footer = Action { implicit request =>
    Ok(views.html.pagelets.footer()).withCss(
      Css("stylesheets/footer.min.css")
    )
  }

  def fallback(name: String)() = Action {
    Ok(views.html.pagelets.fallback(name, "col-md-12"))
  }
}