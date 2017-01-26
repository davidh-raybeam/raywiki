package services.auth

import data.UserRepository

import com.google.inject.{ AbstractModule, Provides }
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.api.{ Environment, Silhouette, SilhouetteProvider, EventBus }
import com.mohiva.play.silhouette.api.util.{ PasswordHasherRegistry, FingerprintGenerator, Clock }
import com.mohiva.play.silhouette.api.crypto.{ CrypterAuthenticatorEncoder, Crypter }
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.util.DefaultFingerprintGenerator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.authenticators.{ SessionAuthenticator, SessionAuthenticatorService, SessionAuthenticatorSettings }
import com.mohiva.play.silhouette.crypto.{ JcaCrypter, JcaCrypterSettings }
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._


class AuthModule extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    bind[Silhouette[Env]].to[SilhouetteProvider[Env]]
    bind[SecuredErrorHandler].to[ErrorHandler]
    bind[UnsecuredErrorHandler].to[ErrorHandler]

    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
    bind[PasswordHasherRegistry].toInstance(
      new PasswordHasherRegistry(new BCryptPasswordHasher())
    )
  }

  @Provides
  def provideEnvironment(
    identityService: UserRepository,
    authenticatorService: AuthenticatorService[SessionAuthenticator],
    eventBus: EventBus
  ): Environment[Env] = {
    Environment[Env](identityService, authenticatorService, Seq.empty, eventBus)
  }

  @Provides
  def provdeSessionAuthenticatorService(
    config: Configuration,
    crypter: Crypter,
    clock: Clock
  ): AuthenticatorService[SessionAuthenticator] = {
    val settings = config.underlying.as[SessionAuthenticatorSettings]("silhouette.authenticator")
    new SessionAuthenticatorService(
      settings,
      new DefaultFingerprintGenerator(false),
      new CrypterAuthenticatorEncoder(crypter),
      clock
    )
  }

  @Provides
  def provideSessionCrypter(
    config: Configuration
  ): Crypter = {
    val settings = config.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    new JcaCrypter(settings)
  }

  @Provides
  def provideAuthInfoRepository(
    userRepo: UserRepository
  ): AuthInfoRepository = {
    new DelegableAuthInfoRepository(userRepo)
  }

  @Provides
  def provideCredentialsProvider(
    authInfoRepo: AuthInfoRepository,
    passwordHashers: PasswordHasherRegistry
  ): CredentialsProvider = {
    new CredentialsProvider(authInfoRepo, passwordHashers)
  }
}
