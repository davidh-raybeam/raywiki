package services.auth

import controllers.routes

import scala.concurrent.Future
import javax.inject.{ Singleton, Inject, Provider }
import play.api.http.DefaultHttpErrorHandler
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import play.api.i18n.{ I18nSupport, MessagesApi, Messages }
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router


@Singleton
class ErrorHandler @Inject() (
  env: Environment,
  config: Configuration,
  sourceMapper: OptionalSourceMapper,
  router: Provider[Router],
  val messagesApi: MessagesApi
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with SecuredErrorHandler with UnsecuredErrorHandler with I18nSupport {
  def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = Future.successful {
    Redirect(routes.SessionController.signIn).flashing("error" -> "must.be.logged.in")
  }

  def onNotAuthorized(implicit request: RequestHeader): Future[Result] = Future.successful{
    Redirect(routes.PageController.page("home")).flashing("error" -> "not.authorized")
  }
}
