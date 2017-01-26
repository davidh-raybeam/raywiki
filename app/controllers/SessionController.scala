package controllers

import forms.{ Login, SignUp }
import data.UserRepository
import models.User
import services.auth.Env

import javax.inject._
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.api.{ Silhouette, LoginInfo, LoginEvent, LogoutEvent, SignUpEvent }
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider


@Singleton
class SessionController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[Env],
  users: UserRepository,
  passwordHasherRegistry: PasswordHasherRegistry
)(implicit
  credentialsProvider: CredentialsProvider
) extends Controller with I18nSupport {
  private val env = silhouette.env

  def signIn = silhouette.UserAwareAction { implicit request =>
    request.identity.fold[Result]{
      Ok(views.html.session.login(Login.form))
    } { _ =>
      Redirect(routes.PageController.page("home")).flashing("info" -> "already.logged.in")
    }
  }

  def handleSignIn = silhouette.UnsecuredAction.async { implicit request =>
    Login.form.bindFromRequest.fold(
      badLogin => Future.successful(BadRequest(views.html.session.login(badLogin))),
      login => {
        (for {
          loginInfo <- credentialsProvider.authenticate(Credentials(login.username, login.password))
          result <- createAuthenticatorAndRedirect(loginInfo)
        } yield {
          env.eventBus.publish(LoginEvent(User(loginInfo), request))
          result
        }).recover {
          case e: ProviderException => {
            val partialLogin = Login.form.fill(login.copy(password = "")).withGlobalError("login.unsuccessful")
            BadRequest(views.html.session.login(partialLogin))
          }
        }
      }
    )
  }

  def signOut = silhouette.SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity.redacted, request))
    env.authenticatorService.discard(request.authenticator,
      Redirect(routes.PageController.page("home")).flashing("info" -> "signed.out")
    )
  }

  def signUp = silhouette.UnsecuredAction { implicit request =>
    Ok(views.html.session.signUp(SignUp.form))
  }

  def handleSignUp = silhouette.UnsecuredAction.async { implicit request =>
    SignUp.form.bindFromRequest.fold(
      badSignUp => Future.successful(BadRequest(views.html.session.signUp(badSignUp))),
      signUp => {
        val partialUser = User.partial(signUp.username)
        val loginInfo = partialUser.loginInfo
        users.retrieve(partialUser.loginInfo).flatMap {
          case Some(_) => {
            val partialSignUp = SignUp.form.fill(
              signUp.copy(password = "", passwordConfirmation = "")
            ).withError("username", "username.already.exists")
            Future.successful(BadRequest(views.html.session.signUp(partialSignUp)))
          }
          case None => {
            for {
              _ <- users.add(loginInfo, passwordHasherRegistry.current.hash(signUp.password))
              result <- createAuthenticatorAndRedirect(loginInfo, successMessage = "signup.successful")
            } yield {
              val eventUser = User(loginInfo)
              env.eventBus.publish(SignUpEvent(eventUser, request))
              env.eventBus.publish(LoginEvent(eventUser, request))
              result
            }
          }
        }
      }
    )
  }

  private def createAuthenticatorAndRedirect(
    loginInfo: LoginInfo,
    successMessage: String = "login.successful"
  )(implicit request: Request[_]): Future[Result] = {
    for {
      authenticator <- env.authenticatorService.create(loginInfo)
      session <- env.authenticatorService.init(authenticator)
      result <- env.authenticatorService.embed(session,
        Redirect(routes.PageController.page("home")).flashing("info" -> Messages(successMessage)))
    } yield result
  }
}
