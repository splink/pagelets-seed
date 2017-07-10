package service

import com.google.inject.Inject
import models.Teaser
import play.api.i18n.Lang

import scala.concurrent.{ExecutionContext, Future}

trait TextblockService {
  def text(implicit lang: Lang, ec: ExecutionContext): Future[Teaser]
}

class TextblockServiceImpl @Inject()(ws: WsConsumer) extends TextblockService with PlayPort {
  override def text(implicit lang: Lang, ec: ExecutionContext) =
    ws.fetch[Teaser](s"http://localhost:$port/mock/textblock")
}