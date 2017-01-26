package controllers

import data.PageRepository
import forms.{ CreatePage, EditPage }
import models.{ Page, User }
import services.auth.Env

import scala.concurrent.Future
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.api.{ Silhouette }
import com.mohiva.play.silhouette.api.actions.{ SecuredRequest, UserAwareRequest }


object PageController {
  case class PageRequest[A](val page: Page, val identity: Option[User], original: Request[A]) extends WrappedRequest[A](original)
}

@Singleton
class PageController @Inject() (
  pages: PageRepository,
  silhouette: Silhouette[Env],
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  import PageController.PageRequest
  private case class LoadPage(id: String) extends ActionRefiner[Request, PageRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, PageRequest[A]]] = {
      val identity: Option[User] = request match {
        case SecuredRequest(user: User, _, _) => Some(user)
        case UserAwareRequest(userOpt, _, _) => userOpt match {
          case Some(user: User) => Some(user)
          case _ => None
        }
        case _ => None
      }
      pages.getPage(id).map { pageOption =>
        pageOption.map { page =>
          PageRequest(page, identity, request)
        }.toRight(NotFound(views.html.errors.notFound(id, identity)))
      }
    }
  }


  def home = Action { implicit request =>
    Redirect(routes.PageController.page("home"))
  }

  def page(id: String) = (silhouette.UserAwareAction andThen LoadPage(id)) { implicit request =>
    Ok(views.html.page.show())
  }

  def newPage = silhouette.SecuredAction { implicit request =>
    Ok(views.html.page.create(CreatePage.form, request.identity))
  }

  def createPage = silhouette.SecuredAction.async { implicit request =>
    CreatePage.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.page.create(formWithErrors, request.identity)))
      },
      createForm => {
        pages.createPage(createForm.id, createForm.content).map { pageId =>
          Redirect(routes.PageController.page(pageId))
        }.recover {
          case e: PageRepository.DuplicatePageException => {
            val formWithErrors = CreatePage.form.fill(createForm).withError("pageid", "pageid.unique")
            BadRequest(views.html.page.create(formWithErrors, request.identity))
          }
        }
      }
    )
  }

  def editPage(id: String) = (silhouette.SecuredAction andThen LoadPage(id)) { implicit request =>
    val editForm = EditPage.form.fill(EditPage(request.page))
    Ok(views.html.page.edit(editForm))
  }

  def updatePage(id: String) = (silhouette.SecuredAction andThen LoadPage(id)).async { implicit request =>
    EditPage.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.page.edit(formWithErrors)))
      },
      editForm => {
        pages.updatePage(request.page.id, editForm.content).map { pageId =>
          Redirect(routes.PageController.page(pageId))
        }
      }
    )
  }
}
