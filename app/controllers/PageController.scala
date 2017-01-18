package controllers

import data.PageRepository

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

@Singleton
class PageController @Inject() (pages: PageRepository) extends Controller {
  case class PageForm(content: String, id: Option[String] = None)

  val createForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "content" -> text
    ){
      (id: String, content: String) => PageForm(content, Some(id))
    }{
      (form: PageForm) => form.id.map { id => (id, form.content) }
    }
  )

  val updateForm = Form(
    mapping(
      "content" -> text
    )(PageForm(_, None))((form: PageForm) => Some(form.content))
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
