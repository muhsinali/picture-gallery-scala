import com.google.inject.AbstractModule
import services.ApplicationInterceptor

/**
  * Module specifies how to bind different types at application startup.
  */
class Module extends AbstractModule {
  override def configure() = {
    // Registered as eager singleton so this is instantiated at application startup.
    bind(classOf[ApplicationInterceptor]).asEagerSingleton()
  }
}
