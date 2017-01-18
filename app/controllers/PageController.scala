package controllers

import data.PageRepository
import forms.PageData

import scala.concurrent.Future
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Singleton
class PageController @Inject() (pages: PageRepository, val messagesApi: MessagesApi) extends Controller with I18nSupport {
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

  def newPage = Action { implicit request =>
    Ok(views.html.newPage(createForm))
  }

  def createPage = Action.async { implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.newPage(formWithErrors)))
      },
      pageData => {
        pages.savePage(pageData.id.get, pageData.content).map { page =>
          Redirect(routes.PageController.page(page.id))
        }
      }
    )
  }
  def editPage(id: String) = TODO
  def updatePage(id: String) = TODO
}
