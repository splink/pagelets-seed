package service

import com.google.inject.Inject
import models.Teaser
import play.api.i18n.Lang

import scala.concurrent.Future

trait TextblockService {
  def text(implicit lang: Lang): Future[Teaser]
}

class TextblockServiceImpl @Inject()(ws: WsConsumer) extends TextblockService {
  override def text(implicit lang: Lang) =
    ws.fetch[Teaser](s"http://localhost:9000/mock/textblock")
}