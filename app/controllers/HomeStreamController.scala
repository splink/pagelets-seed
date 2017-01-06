package controllers

import javax.inject._

import akka.stream.Materializer
import controllers.pagelets._
import org.splink.pagelets._
import org.splink.pagelets.twirl.HtmlStreamOps._
import org.splink.pagelets.twirl.TwirlCombiners._
import play.api.i18n.Messages
import play.api.mvc._
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext

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
                                ec: ExecutionContext,
                                e: Environment,
                                conf: Configuration) extends Controller {
  import carouselPagelet.carousel
  import fallbackPagelet.fallback
  import footerPagelet.footer
  import headerPagelet._
  import pagelets._
  import teaserPagelet.teaser
  import textPagelet.text

  def changeLanguage = headerPagelet.changeLanguage

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
      ),
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
        combineStream(results)(views.stream.pagelets.teasers.apply))
      )),
      Leaf('footer, footer _).withCss(
        Css("stylesheets/footer.min.css")
      )
    ), results =>
    combineStream(results)(views.stream.pagelets.sections.apply))

    request2lang.language match {
      case "de" => tree.skip('text)
      case _ => tree
    }
  }

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def index = PageAction.stream(Messages("title"), tree) { (_, page) =>
    views.stream.wrapper(routes.HomeStreamController.resourceFor)(page)
  }
}