package controllers

import javax.inject._

import akka.stream.Materializer
import controllers.pagelets._
import org.splink.pagelets._
import org.splink.pagelets.twirl.HtmlStreamOps._
import org.splink.pagelets.twirl.TwirlCombiners._
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.api.{Configuration, Environment}


/**
  * This controller is similar to the async HomeController with the difference, that the page is streamed
  * to the client. The controller also shows how the actual pagelets can be kept separate from the
  * controller. This allows to reuse existing pagelets in different controllers.
  *
  * The main API differences are, that
  * * PageAction.stream is used instead of PageAction.async to serve the page
  * * Templates are named template.scala.stream instead of template.scala.html
  * * the combiner function used to combine pagelets in a custom fashion is called combineStream
  */
@Singleton
class HomeStreamController @Inject()(pagelets: Pagelets,
                                     teaserPagelet: TeaserPagelet,
                                     carouselPagelet: CarouselPagelet,
                                     headerPagelet: HeaderPagelet,
                                     footerPagelet: FooterPagelet,
                                     fallbackPagelet: FallbackPagelet,
                                     textPagelet: TextPagelet)(
                                implicit m: Materializer,
                                e: Environment,
                                conf: Configuration) extends InjectedController with I18nSupport {
  import carouselPagelet.carousel
  import fallbackPagelet.fallback
  import footerPagelet.footer
  import headerPagelet._
  import pagelets._
  import teaserPagelet.teaser
  import textPagelet.text

  implicit lazy val executionContext = defaultExecutionContext

  def changeLanguage = headerPagelet.changeLanguage

  def tree(r: RequestHeader) = {
    //make the request implicitly available to the sections combiner template
    implicit val request: RequestHeader = r

    val tree = Tree(Symbol("home"), Seq(
      Leaf(Symbol("header"), header _).
        withJavascript(
          Javascript("lib/bootstrap/js/dropdown.min.js"),
          Javascript("lib/bootstrap/js/alert.min.js")
        ).withMetaTags(
        MetaTag("description", Messages("metaTags.description"))
      ),
      Tree(Symbol("content"), Seq(
        Leaf(Symbol("carousel"), carousel _).
          withJavascript(
            Javascript("lib/bootstrap/js/transition.min.js"),
            Javascript("lib/bootstrap/js/carousel.min.js")).
          withFallback(fallback("Carousel") _),
        Leaf(Symbol("text"), text _).withFallback(fallback("Text") _),
        Tree(Symbol("teasers"), Seq(
          Leaf(Symbol("teaserA"), teaser("A") _),
          Leaf(Symbol("teaserB"), teaser("B") _),
          Leaf(Symbol("teaserC"), teaser("C") _),
          Leaf(Symbol("teaserD"), teaser("D") _)
        ), results =>
        combineStream(results)(views.stream.pagelets.teasers.apply))
      )),
      Leaf(Symbol("footer"), footer _).withCss(
        Css("stylesheets/footer.min.css")
      )
    ), results =>
    combineStream(results)(views.stream.pagelets.sections.apply))

    messagesApi.preferred(r).lang.language match {
      case "de" => tree.skip(Symbol("text"))
      case _ => tree
    }
  }

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def index = PageAction.stream(implicit r => Messages("title"), tree) { (request, page) =>
    views.stream.wrapper(routes.HomeStreamController.resourceFor)(page)(request2Messages(request))
  }
}
