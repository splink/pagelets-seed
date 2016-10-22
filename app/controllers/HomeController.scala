package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.pagelets.TwirlConversions._
import org.splink.pagelets._
import play.api.Environment
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import views.html.{error, wrapper}

@Singleton
class HomeController @Inject()(c: PageletController)(implicit m: Materializer, e: Environment) extends Controller {
  val log = play.api.Logger(getClass).logger

  import c._

  def tree(r: RequestHeader) = {
    val tree = Tree('root, Seq(
      Tree('header, Seq(
        Leaf('navigation, navigation _)
      )),
      Tree('content, Seq(
        Leaf('carousel, carousel _),
        Leaf('text, text _),
        Tree('teasers, Seq(
          Leaf('teaserA, teaser("A") _),
          Leaf('teaserB, teaser("B") _),
          Leaf('teaserC, teaser("C") _),
          Leaf('teaserD, teaser("D") _)
        ), results => combine(results)(views.html.pagelets.teasers.apply))
      )),
      Leaf('footer, footer _)
    ), results => combine(results)(views.html.pagelets.sections.apply))
    tree
  }

  val mainTemplate = wrapper(routes.HomeController.resourceFor) _
  val errorTemplate = error(_)

  def resourceFor(fingerprint: String) = ResourceAction(fingerprint)

  def index = PageAction(errorTemplate)("Index", tree) { (request, page) =>
    log.info(visualize(tree(request)))
    mainTemplate(page)
  }

  def pagelet(id: Symbol) = PageletAction(errorTemplate)(tree, id) { (request, page) =>
    mainTemplate(page)
  }

  def navigation = Action { implicit request =>
    Ok(views.html.pagelets.navigation())
  }

  def carousel = Action { implicit request =>
    Ok(views.html.pagelets.carousel()).
      withJavascript(
        Javascript("lib/bootstrap/js/transition.min.js"),
        Javascript("lib/bootstrap/js/carousel.min.js"))
  }

  def teaser(typ: String)() = Action { implicit request =>
    Ok(views.html.pagelets.teaser(typ))
  }

  def text() = Action { implicit request =>
    Ok(views.html.pagelets.text())
  }

  def footer = Action { implicit request =>
    Ok(views.html.pagelets.footer()).withCss(
      Css("stylesheets/footer.min.css")
    )
  }
}