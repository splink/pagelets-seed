package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.pagelets.TwirlCombiners._
import org.splink.pagelets._
  import org.splink.pagelets.HtmlStreamOps._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}

import scala.concurrent.ExecutionContext
import play.api.mvc._
import play.api.{Configuration, Environment}
import service.{CarouselService, TeaserService, TextblockService}
import views.html.{error, wrapper}

@Singleton
class HomeController @Inject()(pagelets: Pagelets,
                               teaserService: TeaserService,
                               carouselService: CarouselService,
                               textblockService: TextblockService)(
                                implicit m: Materializer,
                                ec: ExecutionContext,
                                e: Environment,
                                conf: Configuration,
                                val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val log = play.api.Logger(getClass).logger

  import pagelets._

  val supportedLanguages = conf.getStringSeq("play.i18n.langs").get

  def tree(r: RequestHeader) = {
    //make the request implicitly available to the sections combiner template
    implicit val request: RequestHeader = r

    val tree = Tree('root, Seq(
      Leaf('header, header _).
        withJavascript(
          Javascript("lib/bootstrap/js/dropdown.min.js"),
          Javascript("lib/bootstrap/js/alert.min.js")
        ).withMetaTags(
        MetaTag("description", Messages("metaTags.description"))
      ).setMandatory(true),
      Tree('content, Seq(
        Leaf('carousel, carousel _).
          withJavascript(
            Javascript("lib/bootstrap/js/transition.min.js"),
            Javascript("lib/bootstrap/js/carousel.min.js")).
          withFallback(fallback("Carousel") _),
        Leaf('text, text _).withFallback(fallback("Text") _),
        Tree('teasers, Seq(
          Leaf('teaserA, teaser("A") _),
          Leaf('teaserB, teaser("B") _),
          Leaf('teaserC, teaser("C") _),
          Leaf('teaserD, teaser("D") _)
        ), results =>
          //combine(results)(views.html.pagelets.teasers.apply))
          combineStream(results)(views.stream.pagelets.teasers.apply))
      )),
      Leaf('footer, footer _).withCss(
        Css("stylesheets/footer.min.css")
      )
    ), results =>
      //combine(results)(views.html.pagelets.sections.apply))
     combineStream(results)(views.stream.pagelets.sections.apply))

    request2lang.language match {
      case "de" => tree.skip('text)
      case _ => tree
    }
  }

  val mainTemplate = wrapper(routes.HomeController.resourceFor) _
  val onError = routes.HomeController.errorPage()

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def stream = PageAction.stream(Messages("title"), tree) { (_, page) =>
    views.stream.wrapper(routes.HomeController.resourceFor)(page)
  }

  def index = PageAction.async(onError)(Messages("title"), tree) { (request, page) =>
    log.info("\n" + visualize(tree(request)))
    mainTemplate(page)
  }

  def errorPage = Action { implicit request =>
    val language = request.cookies.get(messagesApi.langCookieName).
      map(_.value).getOrElse(supportedLanguages.head)

    InternalServerError(error(language))
  }

  def pagelet(id: Symbol) = PageletAction.async(onError)(tree, id) { (_, page) =>
    mainTemplate(page)
  }

  val langForm = Form(single("language" -> nonEmptyText))

  def changeLanguage = Action { implicit request =>
    val target = request.headers.get(REFERER).getOrElse(routes.HomeController.index().path)

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

  def carousel = Action.async { implicit request =>
    carouselService.carousel.map { teasers =>
      throw new Exception("test")
      Ok(views.html.pagelets.carousel(teasers)).
        withCookies(Cookie("carouselCookie", "carouselValue"))
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
      Ok(views.html.pagelets.text(text)).
      withCookies(Cookie("textCookie", "textValue"))
    }
  }

  def footer = Action { implicit request =>
    Ok(views.html.pagelets.footer())
  }

  def fallback(name: String)() = Action {
    Ok(views.html.pagelets.fallback(name, "col-md-12"))
  }
}