package controllers.forms

import play.api.data._
import play.api.data.Forms._

case class SignUp(username: String, password: String, passwordConfirmation: String)

object SignUp {
  val form = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "passwordConfirmation" -> nonEmptyText
    )(SignUp.apply)(SignUp.unapply) verifying("passwords.must.match", _ match {
      case SignUp(_, password, confirmation) => password == confirmation
    })
  )
}
