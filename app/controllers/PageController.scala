package controllers

import data.PageRepository
import forms.PageData

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

@Singleton
class PageController @Inject() (pages: PageRepository) extends Controller {

  val createForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "content" -> text
    ){
      (id: String, content: String) => PageData(content, Some(id))
    }{
      (form: PageData) => form.id.map { id => (id, form.content) }
    }
  )

  val updateForm = Form(
    mapping(
      "content" -> text
    )(PageData(_, None))((form: PageData) => Some(form.content))
  )

  def home = Action { implicit request =>
    Redirect(routes.PageController.page("home"))
  }

  def page(id: String) = Action { implicit request =>
    pages.getPage(id).fold[Result](NotFound) { page =>
      val navigation = pages.getPage("navigation").map(_.content)
      Ok(views.html.page(page, navigation))
    }
  }

  def newPage = TODO
  def createPage = TODO
  def editPage(id: String) = TODO
  def updatePage(id: String) = TODO
}
