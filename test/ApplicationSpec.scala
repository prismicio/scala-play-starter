package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
object ApplicationSpec extends Specification {

  "Prismic.io application" should {
    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      route(FakeRequest(GET, "/")) must beSome.which { res =>
        status(res) must_== OK and (
          contentType(res) must beSome.which(_ == "text/html")) and (
          contentAsString(res) must contain("ganache-specialist"))

      }
    }

    "render the not-found page" in new WithApplication {
      route(FakeRequest(GET, "/not-found")) must beSome.which { res =>
        status(res) must_== NOT_FOUND and (
          contentType(res) must beSome.which(_ == "text/html")) and (
          contentAsString(res) must contain("Page not found"))

      }
    }

    "render the search page" in new WithApplication {
      route(FakeRequest(GET, "/search")) must beSome.which { res =>
        status(res) must_== OK and (
          contentType(res) must beSome.which(_ == "text/html")) and (
          contentAsString(res) must contain("fruit-expert"))

      }
    }

    "render the vanilla macaron document" in new WithApplication {
      route(FakeRequest(GET, "/documents/UlfoxUnM0wkXYXbH/vanilla-macaron")).
        aka("result") must beSome.which { res =>
          status(res) must_== OK and (
            contentType(res) must beSome.which(_ == "text/html")) and (
            contentAsString(res).
              aka("content") must contain("pure extract of Madagascar vanilla"))

        }
    }
  }
}
