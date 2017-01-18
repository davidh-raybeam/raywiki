package controllers

import models.Page
import data.PageRepository

import org.scalatestplus.play._
import org.scalatest.mock.MockitoSugar
import play.api.test._
import play.api.test.Helpers._
import org.mockito.Mockito._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class PageControllerSpec extends PlaySpec with OneAppPerTest with MockitoSugar {

  "PageController GET /" should {

    "redirect to the home page" in {
      val controller = app.injector.instanceOf[PageController]
      val home = controller.home().apply(FakeRequest())

      redirectLocation(home) mustBe Some(routes.PageController.page("home").url)
    }

  }

  "PageController GET /pages/:id" should {

    "render the page with the given id" in {
      val mockPageRepository = mock[PageRepository]
      when(mockPageRepository.getPage("foo")).thenReturn(Some(Page("foo", "foo title", "foo content")))
      when(mockPageRepository.getPage("navigation")).thenReturn(None)

      val controller = new PageController(mockPageRepository)
      val pageResponse = controller.page("foo").apply(FakeRequest())
      val renderedPage = contentAsString(pageResponse)

      renderedPage must include ("foo title")
      renderedPage must include ("foo content")
    }

  }
}
