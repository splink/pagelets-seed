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

    val tree = Tree("home".id, Seq(
      Leaf("header".id, () => header).
        withJavascript(
          Javascript("lib/bootstrap/js/dropdown.min.js"),
          Javascript("lib/bootstrap/js/alert.min.js")
        ).withMetaTags(
        MetaTag("description", Messages("metaTags.description"))
      ),
      Tree("content".id, Seq(
        Leaf("carousel".id, () => carousel).
          withJavascript(
            Javascript("lib/bootstrap/js/transition.min.js"),
            Javascript("lib/bootstrap/js/carousel.min.js")).
          withFallback(fallback("Carousel") _),
        Leaf("text".id, () => text).withFallback(fallback("Text") _),
        Tree("teasers".id, Seq(
          Leaf("teaserA".id, teaser("A") _),
          Leaf("teaserB".id, teaser("B") _),
          Leaf("teaserC".id, teaser("C") _),
          Leaf("teaserD".id, teaser("D") _)
        ), results =>
        combineStream(results)(views.stream.pagelets.teasers.apply))
      )),
      Leaf("footer".id, () => footer).withCss(
        Css("stylesheets/footer.min.css")
      )
    ), results =>
    combineStream(results)(views.stream.pagelets.sections.apply))

    messagesApi.preferred(r).lang.language match {
      case "de" => tree.skip("text".id)
      case _ => tree
    }
  }

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def index = PageAction.stream(implicit r => Messages("title"), tree) { (request, page) =>
    views.stream.wrapper(routes.HomeStreamController.resourceFor)(page)(request2Messages(request))
  }
}
