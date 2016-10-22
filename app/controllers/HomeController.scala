package controllers

import javax.inject._

import akka.stream.Materializer
import org.splink.pagelets.TwirlConversions._
import org.splink.pagelets._
import play.api.Environment
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import views.html.{error, wrapper}

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(c: PageletController)(implicit m: Materializer, e: Environment) extends Controller {
  val log = play.api.Logger(getClass).logger

  import c._

  def tree(r: RequestHeader) = {
    val tree = Tree('root, Seq(
      Tree('header, Seq(
        Leaf('navigation, navigation _).withFallback(navigationFallback _)
      )),
      Tree('content, Seq(
        Leaf('carousel, carousel _),
        Tree('texts, Seq(
          Leaf('textA, text("A") _),
          Leaf('textB, text("B") _)
        ), results => combine(results)(views.html.pagelets.sixsix.apply))
      )),
      Leaf('footer, footer _)
    ), results => combine(results)(views.html.sections.apply))
    tree/*.replace('brick2, Leaf('yo, yo _))*/
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

  def navigation = Action.async { implicit request =>
    Future {
      Ok(views.html.navigation()).
        withJavascript(Javascript("lib/bootstrap/js/collapse.js"))
    }
  }

  def carousel = Action { implicit request =>
    Ok(views.html.pagelets.carousel()).
      withJavascript(
        Javascript("lib/bootstrap/js/transition.min.js"),
        Javascript("lib/bootstrap/js/carousel.js"))
  }

  def text(typ: String)() = Action { implicit request =>
    Ok(views.html.pagelets.text(typ))
  }

  def footer = Action { implicit request =>
    Ok(views.html.pagelets.footer()).withCss(
      Css("stylesheets/footer.min.css")
    )
  }

  def navigationFallback = Action {
    Ok("navigationFallback")
  }
}