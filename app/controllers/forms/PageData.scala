package controllers.forms

import models.Page

case class NewPageRequest(id: String, content: String)

case class PageEditRequest(content: String)

object PageEditRequest {
  def apply(p: Page): PageEditRequest = PageEditRequest(p.rawContent)
}
