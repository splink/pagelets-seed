import com.google.inject.AbstractModule
import service._

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[WsConsumer]).to(classOf[WsConsumerImpl]).asEagerSingleton()
    bind(classOf[TeaserService]).to(classOf[TeaserServiceImpl]).asEagerSingleton()
    bind(classOf[CarouselService]).to(classOf[CarouselServiceImpl]).asEagerSingleton()
    bind(classOf[TextblockService]).to(classOf[TextblockServiceImpl]).asEagerSingleton()
  }
}