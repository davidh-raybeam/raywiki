package controllers.forms

import models.Page

import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.Forms._

case class EditPage(content: String)

object EditPage {
  def form = Form(
    mapping(
      "content" -> text
    )(EditPage.apply)(EditPage.unapply)
  )

  def apply(p: Page): EditPage = EditPage(p.rawContent)
}
