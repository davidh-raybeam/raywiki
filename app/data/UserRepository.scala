package data

import models.User

import javax.inject._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.SQLiteDriver.api._
import slick.driver.SQLiteDriver
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO


object UserRepository {
  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def passwordHash = column[String]("password_hash")
    def salt = column[String]("salt")

    def * = (username, passwordHash, salt, id.?) <> (User.tupled, User.unapply)
  }

  def users = TableQuery[UserTable]


  def byUsername(username: String) = users.filter(_.username === username).result

  def create(user: User) =
    (users
      returning users.map(_.id)
      into ((user, id) => user.copy(id = Some(id)))
    ) += user

  def updatePassword(updated: User) = {
    val toUpdate = for {
      user <- users
      if user.username === updated.username
    } yield (user.passwordHash, user.salt)
    toUpdate.update((updated.passwordHash, updated.salt))
  }
}

@Singleton
class UserRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider
) extends DelegableAuthInfoDAO[PasswordInfo] with IdentityService[User] {
  private val dbConfig = dbConfigProvider.get[SQLiteDriver]
  private val db = dbConfig.db
  import UserRepository._

  def getUser(username: String): Future[Option[User]] = db.run {
    byUsername(username).headOption
  }

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    getUser(loginInfo.providerKey)


  def add(loginInfo: LoginInfo, auth: PasswordInfo): Future[PasswordInfo] =
    db.run { create(User(loginInfo, auth)) }.map(_.authInfo)

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    retrieve(loginInfo).map(_.map(_.authInfo))

  def remove(loginInfo: LoginInfo): Future[Unit] =
    Future.failed(new UnsupportedOperationException("Removing auth info from users is not supported"))

  def save(loginInfo: LoginInfo, auth: PasswordInfo): Future[PasswordInfo] =
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, auth)
      case None => add(loginInfo, auth)
    }

  def update(loginInfo: LoginInfo, auth: PasswordInfo): Future[PasswordInfo] =
    db.run {
      updatePassword(User(loginInfo, auth))
    }.map( _ => auth )
}
