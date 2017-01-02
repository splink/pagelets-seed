package service

import com.google.inject.Inject
import models.Teaser
import play.api.i18n.Lang

import scala.concurrent.Future


trait TeaserService {
  def teaser(typ: String)(implicit lang: Lang): Future[Teaser]
}

class TeaserServiceImpl @Inject()(ws: WsConsumer) extends TeaserService with PlayPort {
  override def teaser(typ: String)(implicit lang: Lang) =
    ws.fetch[Teaser](s"http://localhost:$port/mock/teaser/$typ")
}