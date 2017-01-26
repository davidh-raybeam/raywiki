package controllers.forms

import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.Forms._

case class CreatePage(id: String, content: String)

object CreatePage {
  def form = Form(
    mapping(
      "pageid" -> nonEmptyText
        .verifying(pattern("[a-z0-9]+".r, name="pageid.pattern", error="pageid.pattern.error")),
      "content" -> text
    )(CreatePage.apply)(CreatePage.unapply)
  )
}
