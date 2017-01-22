package controllers

import data.PageRepository
import forms._
import models.Page

import scala.concurrent.Future
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


object PageController {
  case class Navigation(val content: Option[String])
  case class RequestWithNavigation[A](navigation: Navigation, original: Request[A]) extends WrappedRequest[A](original)
  case class PageRequest[A](val page: Page, val navigation: Navigation, original: Request[A]) extends WrappedRequest[A](original)
}

@Singleton
class PageController @Inject() (pages: PageRepository, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val createForm = Form(
    mapping(
      "pageid" -> nonEmptyText
        .verifying(pattern("[a-z0-9]+".r, name="pageid.pattern", error="pageid.pattern.error")),
      "content" -> text
    )(NewPageRequest.apply)(NewPageRequest.unapply)
  )

  val updateForm = Form(
    mapping(
      "content" -> text
    )(PageEditRequest.apply)(PageEditRequest.unapply)
  )

  import PageController._
  private object LoadNavigation extends ActionBuilder[RequestWithNavigation] with ActionTransformer[Request, RequestWithNavigation]{
    def transform[A](request: Request[A]): Future[RequestWithNavigation[A]] = {
      for {
        navPage <- pages.getPage("navigation")
        navigation = Navigation(navPage.map(_.content))
      } yield RequestWithNavigation(navigation, request)
    }
  }
  private case class LoadPage(id: String) extends ActionRefiner[RequestWithNavigation, PageRequest] {
    def refine[A](request: RequestWithNavigation[A]): Future[Either[Result, PageRequest[A]]] = {
      pages.getPage(id).map { pageOption =>
        pageOption.map { page =>
          PageRequest(page, request.navigation, request)
        }.toRight(NotFound.as(HTML))
      }
    }
  }
  private def NavigationAction = LoadNavigation
  private def PageAction[A](id: String) = LoadNavigation andThen LoadPage(id)


  def home = Action { implicit request =>
    Redirect(routes.PageController.page("home"))
  }

  def page(id: String) = PageAction(id) { implicit request =>
    Ok(views.html.page())
  }

  def newPage = NavigationAction { implicit request =>
    implicit val navigation = request.navigation
    Ok(views.html.newPage(createForm))
  }

  def createPage = NavigationAction.async { implicit request =>
    implicit val navigation = request.navigation
    createForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.newPage(formWithErrors)))
      },
      createRequest => {
        pages.createPage(createRequest.id, createRequest.content).map { pageId =>
          Redirect(routes.PageController.page(pageId))
        }.recover {
          case e: PageRepository.DuplicatePageException => {
            val formWithErrors = createForm.bindFromRequest.withError("pageid", "pageid.unique")
            BadRequest(views.html.newPage(formWithErrors))
          }
        }
      }
    )
  }

  def editPage(id: String) = PageAction(id) { implicit request =>
    val editForm = updateForm.fill(PageEditRequest(request.page))
    Ok(views.html.editPage(editForm))
  }

  def updatePage(id: String) = PageAction(id).async { implicit request =>
    updateForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.editPage(formWithErrors)))
      },
      updateRequest => {
        pages.updatePage(request.page.id, updateRequest.content).map { pageId =>
          Redirect(routes.PageController.page(pageId))
        }
      }
    )
  }
}
