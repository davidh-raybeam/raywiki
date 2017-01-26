package controllers.forms

import play.api.data._
import play.api.data.Forms._

case class Login(username: String, password: String)

object Login {
  lazy val form = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )
}
