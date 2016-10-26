package service

import com.google.inject.Inject
import models.Teaser
import play.api.i18n.Lang

import scala.concurrent.Future

trait CarouselService {
  def carousel(implicit lang: Lang): Future[Seq[Teaser]]
}

class CarouselServiceImpl @Inject()(ws: WsConsumer) extends CarouselService {
  override def carousel(implicit lang: Lang) =
    ws.fetch[Seq[Teaser]](s"http://localhost:9000/mock/carousel")
}