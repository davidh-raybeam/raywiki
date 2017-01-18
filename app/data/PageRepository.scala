package data

import models.Page
import controllers.routes

import javax.inject._
import play.api._
import scala.io.Source
import laika.api._
import laika.parse.markdown.Markdown
import laika.render.HTML
import laika.tree.Documents.Document
import laika.tree.Elements.{RewriteRule, ExternalLink}

@Singleton
class PageRepository @Inject() (config: Configuration) {
  private lazy val parse = Parse as Markdown
  private lazy val render = Render as HTML

  private val PageUrl = "^page:(.*)$".r
  private val linkRewriter: RewriteRule = {
    case link@ExternalLink(_, PageUrl(pageId), _, _) => {
      val pageUrl = routes.PageController.page(pageId).url
      Some(link.copy(url=pageUrl))
    }
  }

  private def fileForPage(id: String) =
    for {
      rootPath <- config.getString("pages.root")
    } yield new java.io.File(rootPath, s"${id}.md")

  private def parsedContentsOfPage(id: String) =
    for {
      file <- fileForPage(id)
      if file.exists
    } yield parse.fromFile(file).rewrite(linkRewriter)

  private def renderTitle(doc: Document) =
    doc.title.map(render.from(_).toString).mkString

  private def renderBody(doc: Document) =
    render.from(doc).toString

  def getPage(id: String): Option[Page] =
    for {
      doc <- parsedContentsOfPage(id)
    } yield Page(id, renderTitle(doc), renderBody(doc))
}
