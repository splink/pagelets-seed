import controllers.HomeController
import models.{Image, Size, Teaser}
import org.scalatest.BeforeAndAfter
import org.mockito.scalatest.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import org.splink.pagelets.{PageletId, PageletsAssembly}
import play.api.Environment
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Call, ControllerComponents, Cookie}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.test.CSRFTokenHelper._
import play.twirl.api.Html
import service.{CarouselService, TeaserService, TextblockService}

import scala.concurrent.{ExecutionContext, Future}

class HomeControllerTest extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfter {
  implicit lazy val mat = app.materializer
  implicit lazy val env = Environment.simple()
  implicit lazy val conf = app.configuration
  implicit lazy val msg = app.injector.instanceOf[MessagesApi]

  val pagelets = new PageletsAssembly {
    override lazy val controllerComponents = app.injector.instanceOf[ControllerComponents]
  }

  var teaserService: TeaserService = _
  var carouselService: CarouselService = _
  var textblockService: TextblockService = _
  var ctrl: HomeController = _

  before {
    teaserService = mock[TeaserService]
    carouselService = mock[CarouselService]
    textblockService = mock[TextblockService]

    ctrl = new HomeController(pagelets, teaserService, carouselService, textblockService) {
      setControllerComponents(pagelets.controllerComponents)
    }
  }

  val teaser = Future.successful {
    Teaser("title", "text", Image("http://image.png", Size(1, 1), "alt"))
  }

  val teasers = {
    import scala.concurrent.ExecutionContext.Implicits.global
    teaser.map(t => Seq(t))
  }

  "HomeController#index" should {

    "serve the german home page" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "de")).withCSRFToken

      when(teaserService.teaser(any[String])(any[Lang], any[ExecutionContext])).thenReturn(teaser)
      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(teasers)
      when(textblockService.text(any[Lang], any[ExecutionContext])).thenReturn(teaser)

      val result = ctrl.index()(request)
      contentAsString(result) must include("""carousel-control""")
      contentAsString(result) must include("""lang="de"""")
      status(result) must equal(OK)
    }

    "serve the english home page" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "en")).withCSRFToken

      when(teaserService.teaser(any[String])(any[Lang], any[ExecutionContext])).thenReturn(teaser)
      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(teasers)
      when(textblockService.text(any[Lang], any[ExecutionContext])).thenReturn(teaser)

      val result = ctrl.index()(request)
      contentAsString(result) must include("""carousel-control""")
      contentAsString(result) must include("""lang="en"""")
      status(result) must equal(OK)
    }

    "serve the home page, even if some pagelets fail" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "en")).withCSRFToken

      when(teaserService.teaser(any[String])(any[Lang], any[ExecutionContext])).thenReturn(teaser)
      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException("Carousel failure")))
      when(textblockService.text(any[Lang], any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException("Text failure")))

      val result = ctrl.index()(request)
      contentAsString(result) must include("""<h2>Fallback <small>Carousel""")
      contentAsString(result) must include("""lang="en"""")
      status(result) must equal(OK)
    }

    "redirect to an error page if an exception occurs within the index action" in {
      when(teaserService.teaser(any[String])(any[Lang], any[ExecutionContext])).thenReturn(teaser)
      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(teasers)
      when(textblockService.text(any[Lang], any[ExecutionContext])).thenReturn(teaser)

      val ctrl = new HomeController(pagelets, teaserService, carouselService, textblockService) {
        import pagelets._

        setControllerComponents(pagelets.controllerComponents)

        override def index = PageAction.async[Html](Call("GET", "/error"))(_ => "title", tree) { (request, page) =>
          throw new RuntimeException("error")
        }
      }

      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "en")).withCSRFToken

      val result = ctrl.index(request)
      status(result) must equal(TEMPORARY_REDIRECT)
    }
  }

  "HomeController#pagelet" should {
    "serve the carousel pagelet in german" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "de")).withCSRFToken

      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(teasers)

      val result = ctrl.pagelet(PageletId("carousel"))(request)
      contentAsString(result) must include("""lang="de"""")
      contentAsString(result) must include("""carousel""")
      status(result) must equal(OK)
    }

    "serve the carousel pagelet in english" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "en")).withCSRFToken

      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(teasers)

      val result = ctrl.pagelet(PageletId("carousel"))(request)
      contentAsString(result) must include("""lang="en"""")
      contentAsString(result) must include("""carousel""")
      status(result) must equal(OK)
    }

    "serve the fallback for the carousel pagelet if it fails" in {
      val request = FakeRequest().withCookies(Cookie(msg.langCookieName, "en")).withCSRFToken

      when(carouselService.carousel(any[Lang], any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException("Carousel failure")))

      val result = ctrl.pagelet(PageletId("carousel"))(request)
      contentAsString(result) must include("""lang="en"""")
      contentAsString(result) must include("""Fallback""")
      status(result) must equal(OK)
    }
  }

  "HomeController#changeLanguage" should {
    "set the new language in a Cookie and redirect to the index action" in {
      val request = FakeRequest("POST", "/changeLanguage").
        withFormUrlEncodedBody("language" -> "de").
        withCookies(Cookie(msg.langCookieName, "en")).
        withCSRFToken

      val result = call(ctrl.changeLanguage, request)
      cookies(result).apply(msg.langCookieName).value must equal("de")
      status(result) must equal(SEE_OTHER)
    }

    "return a BadRequest status of 400 if the language does not exist" in {
      val request = FakeRequest("POST", "/changeLanguage").
        withFormUrlEncodedBody("language" -> "es").
        withCookies(Cookie(msg.langCookieName, "en")).
        withCSRFToken

      val result = call(ctrl.changeLanguage, request)
      status(result) must equal(BAD_REQUEST)
    }

    "return a BadRequest status of 400 if the language change post does not validate" in {
      val request = FakeRequest("POST", "/changeLanguage").
        withCookies(Cookie(msg.langCookieName, "en")).
        withCSRFToken

      val result = call(ctrl.changeLanguage, request)
      status(result) must equal(BAD_REQUEST)
    }
  }
}
