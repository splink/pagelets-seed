package service

import com.google.inject.Inject
import models.Teaser
import play.api.i18n.Lang

import scala.concurrent.{ExecutionContext, Future}

trait CarouselService {
  def carousel(implicit lang: Lang, ec: ExecutionContext): Future[Seq[Teaser]]
}

class CarouselServiceImpl @Inject()(ws: WsConsumer) extends CarouselService with PlayPort {
  override def carousel(implicit lang: Lang, ec: ExecutionContext) =
    ws.fetch[Seq[Teaser]](s"http://localhost:$port/mock/carousel")
}