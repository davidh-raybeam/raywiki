package controllers

import data.PageRepository

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class PageController @Inject() (pages: PageRepository) extends Controller {

  def home = Action { implicit request =>
    Redirect(routes.PageController.page("home"))
  }

  def page(id: String) = Action { implicit request =>
    pages.getPage(id).fold[Result](NotFound) { page =>
      val navigation = pages.getPage("navigation").map(_.content)
      Ok(views.html.page(page, navigation))
    }
  }
}
