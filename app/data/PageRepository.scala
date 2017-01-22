package data

import models.Page
import controllers.routes

import javax.inject._
import java.io.FileWriter
import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick.DatabaseConfigProvider
import scala.io.Source
import scala.concurrent.Future
import laika.api._
import laika.parse.markdown.Markdown
import laika.render.HTML
import laika.tree.Documents.Document
import laika.tree.Elements.{RewriteRule, ExternalLink}
import slick.driver.SQLiteDriver.api._
import slick.driver.SQLiteDriver

object PageRepository {
  private lazy val parse = Parse as Markdown
  private lazy val render = Render as HTML

  private val PageUrl = "^page:(.*)$".r
  private val linkRewriter: RewriteRule = {
    case link@ExternalLink(_, PageUrl(pageId), _, _) => {
      val pageUrl = routes.PageController.page(pageId).url
      Some(link.copy(url=pageUrl))
    }
  }

  private def parseDocument(rawContent: String) =
    parse.fromString(rawContent).rewrite(linkRewriter)

  private def renderBody(rawContent: String) =
    render.from(parseDocument(rawContent)).toString

  private def renderTitle(rawContent: String) =
    parseDocument(rawContent).title.map(render.from(_).toString).mkString

  private def dbRowToPage(row: (String, String, String)) = row match {
    case (id, title, rawContent) =>
      Page(id, title, renderBody(rawContent), rawContent)
  }

  private def pageToDbRow(page: Page): Option[(String, String, String)] =
    Some((page.id, page.title, page.rawContent))


  class PageTable(tag: Tag) extends Table[Page](tag, "pages") {
    def id = column[String]("page_id", O.PrimaryKey)
    def title = column[String]("title")
    def rawContent = column[String]("content")

    def * = (id, title, rawContent) <> (dbRowToPage, pageToDbRow)
  }
  val pages = TableQuery[PageTable]

  def byId(id: String) = pages.filter(_.id === id)

  def insert(id: String, rawContent: String): DBIO[Int] = {
    val page = Page(id, renderTitle(rawContent), "", rawContent)
    pages += page
  }

  def update(id: String, rawContent: String): DBIO[Int] = {
    val title = renderTitle(rawContent)
    byId(id).map(p => (p.title, p.rawContent)).update((title, rawContent))
  }

  case class DuplicatePageException(id: String)
    extends RuntimeException(s"A page with id $id already exists.")
}

@Singleton
class PageRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[SQLiteDriver]
  private val db = dbConfig.db
  import PageRepository._

  def getPage(id: String): Future[Option[Page]] =
    db.run(pages.filter(_.id === id).result.headOption)

  def pageExists(id: String): Future[Boolean] =
    db.run(pages.filter(_.id === id).exists.result)

  def createPage(id: String, rawContent: String): Future[String] =
    pageExists(id).flatMap { pageAlreadyExists =>
      if (pageAlreadyExists)
        Future.failed(DuplicatePageException(id))
      else
        db.run(insert(id, rawContent)).map(_ => id)
    }

  def updatePage(id: String, rawContent: String): Future[String] =
    db.run(update(id, rawContent)).map(_ => id)
}
