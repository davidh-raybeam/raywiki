package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo, Provider }
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.password.BCryptPasswordHasher

case class User(
  username: String,
  passwordHash: String,
  salt: String,
  id: Option[Long] = None
) extends Identity {
  def loginInfo(implicit provider: Provider): LoginInfo = LoginInfo(provider.id, username)
  def authInfo: PasswordInfo = PasswordInfo(BCryptPasswordHasher.ID, passwordHash, Some(salt))
  def redacted: User = copy(passwordHash="password hash omitted", salt="salt omitted")
}

object User extends ((String, String, String, Option[Long]) => User) {
  def apply(loginInfo: LoginInfo, authInfo: PasswordInfo): User =
    User(loginInfo.providerKey, authInfo.password, authInfo.salt getOrElse "no salt")

  def apply(loginInfo: LoginInfo): User =
    partial(loginInfo.providerKey).redacted

  def partial(username: String): User = User(username, "", "")
}
