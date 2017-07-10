package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.pagelets._
import org.splink.pagelets.twirl.TwirlCombiners._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc._
import play.api.{Configuration, Environment}
import service.{CarouselService, TeaserService, TextblockService}
import views.html.{error, wrapper}

import scala.concurrent.ExecutionContext


/**
  * A controller which shows Pagelets in async mode. Async mode renders everything on the server
  * side before the complete page is actually sent to the client.
  */
@Singleton
class HomeController @Inject()(pagelets: Pagelets,
                               teaserService: TeaserService,
                               carouselService: CarouselService,
                               textblockService: TextblockService)(
                                implicit m: Materializer,
                                e: Environment,
                                conf: Configuration) extends InjectedController with I18nSupport {
  val log = play.api.Logger(getClass).logger

  implicit lazy val executionContext = defaultExecutionContext

  import pagelets._

  val supportedLanguages = conf.get[Seq[String]]("play.i18n.langs")

  implicit def request2lang(implicit r: RequestHeader) = messagesApi.preferred(r).lang

  // the page configuration
  def tree(r: RequestHeader) = {
    //make the request implicitly available to the sections combiner template
    implicit val request: RequestHeader = r

    val tree = Tree('home, Seq(
      Leaf('header, header _).
        withJavascript(
          Javascript("lib/bootstrap/js/dropdown.min.js"),
          Javascript("lib/bootstrap/js/alert.min.js")
        ).withMetaTags(
        MetaTag("description", Messages("metaTags.description"))
        // the header is mandatory. If it fails, the user is redirected to an error page @see the index Action
      ).setMandatory(true),
      Tree('content, Seq(
        Leaf('carousel, carousel _).
          // the carousel pagelet depends on specific Javascripts
          withJavascript(
            Javascript("lib/bootstrap/js/transition.min.js"),
            Javascript("lib/bootstrap/js/carousel.min.js")).
          withFallback(fallback("Carousel") _),
        // if the text pagelet fails, the fallback pagelet is rendered, if no fallback is defined, the pagelet is left out
        Leaf('text, text _).withFallback(fallback("Text") _),
        Tree('teasers, Seq(
          Leaf('teaserA, teaser("A") _),
          Leaf('teaserB, teaser("B") _),
          Leaf('teaserC, teaser("C") _),
          Leaf('teaserD, teaser("D") _)
        ), results =>
          // the usage of a combine template, which allows to control how the child pagelets are put together
          combine(results)(views.html.pagelets.teasers.apply))
      )),
      Leaf('footer, footer _).withCss(
        // the footer pagelet depends on specific Javascripts
        Css("stylesheets/footer.min.css")
      )
    ), results =>
      combine(results)(views.html.pagelets.sections.apply))

    // output a different for users who prefer to view the page in german
    request2lang.language match {
      case "de" => tree.skip('text)
      case _ => tree
    }
  }

  def mainTemplate(implicit r: RequestHeader) = wrapper(routes.HomeController.resourceFor) _
  val onError = routes.HomeController.errorPage()

  // action to send a combined resource (that is Javascript or Css) for a fingerprint
  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  // the main Action which triggers the rendering of the page according to the tree
  def index = PageAction.async(onError)(implicit r => Messages("title"), tree) { (request, page) =>
    // uncomment to log a visualization of the tree
    //log.info("\n" + visualize(tree(request)))
    mainTemplate(request)(page)
  }

  // in case a mandatory pagelet produces an error, the user is redirected to the error page
  def errorPage = Action { implicit request =>
    val language = request.cookies.get(messagesApi.langCookieName).
      map(_.value).getOrElse(supportedLanguages.head)

    InternalServerError(error(language))
  }

  // render any part of the page tree. For instance just the footer, or the whole content
  def pagelet(id: Symbol) = PageletAction.async(onError)(tree, id) { (request, page) =>
    mainTemplate(request)(page)
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

  // a pagelet is just a simple Play Action
  def carousel = Action.async { implicit request =>
    carouselService.carousel.map { teasers =>
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