package controllers

import data.PageRepository
import forms._

import scala.concurrent.Future
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Singleton
class PageController @Inject() (pages: PageRepository, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val createForm = Form(
    mapping(
      "pageid" -> nonEmptyText
        .verifying(pattern("[a-z0-9]+".r, name="pageid.pattern", error="pageid.pattern.error"))
        .verifying("pageid.unique", !pages.pageExists(_)),
      "content" -> text
    )(NewPageRequest.apply)(NewPageRequest.unapply)
  )

  val updateForm = Form(
    mapping(
      "content" -> text
    )(PageEditRequest.apply)(PageEditRequest.unapply)
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
      createRequest => {
        pages.savePage(createRequest.id, createRequest.content).map { page =>
          Redirect(routes.PageController.page(page.id))
        }
      }
    )
  }

  def editPage(id: String) = Action { implicit request =>
    pages.getPage(id).fold[Result](NotFound) { page =>
      val editForm = updateForm.fill(PageEditRequest(page))
      Ok(views.html.editPage(page, editForm))
    }
  }

  def updatePage(id: String) = Action.async { implicit request =>
    pages.getPage(id).fold[Future[Result]](Future.successful(NotFound)) { page =>
      updateForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.editPage(page, formWithErrors)))
        },
        updateRequest => {
          pages.savePage(page.id, updateRequest.content).map { _ =>
            Redirect(routes.PageController.page(page.id))
          }
        }
      )
    }
  }
}
