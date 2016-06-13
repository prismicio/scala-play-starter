package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.Configuration

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import io.prismic._

case class Context(
  endpoint: String,
  api: Api
) {
  // -- Resolve links to documents
  def linkResolver(implicit request: RequestHeader) = DocumentLinkResolver(api) {
    case (docLink, maybeBookmarked) if !docLink.isBroken => routes.Application.detail(docLink.id, docLink.slug).absoluteURL()
    case _ => routes.Application.brokenLink().absoluteURL()
  }

}


@Singleton
class Application @Inject() (configuration: Configuration) extends Controller {

  // -- Cache to use (default to keep 200 JSON responses in a LRU cache)
  private val Cache = BuiltInCache(200)

  // -- Write debug and error messages to the Play `prismic` logger (check the configuration in application.conf)
  private val Logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic").debug(message)
    case 'ERROR => play.api.Logger("prismic").error(message)
    case _      => play.api.Logger("prismic").info(message)
  }

  private def endpoint = configuration.getString("prismic.api").getOrElse(sys.error(s"Missing configuration prismic.api"))
  private def token = configuration.getString("prismic.token")
  private def fetchApi: Future[Api] = Api.get(endpoint, accessToken = token, cache = Cache, logger = Logger)

  private def ref(api: Api)(implicit request: RequestHeader): String = {
    val experimentRef: Option[String] = request.cookies.get(Prismic.experimentsCookie).map(_.value).flatMap(api.experiments.refFromCookie)
    val previewRef: Option[String] = request.cookies.get(Prismic.previewCookie).map(_.value)
    previewRef.orElse(experimentRef).getOrElse(api.master.ref)
  }

  def ctx(api: Api) = Context(endpoint, api)

  // -- Page not found
  def PageNotFound = NotFound(views.html.pageNotFound())

  def brokenLink = Action { implicit request =>
    PageNotFound
  }

  // -- Home page
  def index(page: Int) = Action.async { implicit request =>
    for {
      api <- fetchApi
      response <- api.forms("everything").ref(ref(api)).pageSize(10).page(page).submit()
    } yield {
      Ok(views.html.index(response)(ctx(api)))
    }
  }

  // -- Document detail
  def detail(id: String, slug: String) = Action.async { implicit request =>
    for {
      api <- fetchApi
      context = ctx(api)
      maybeDocument <- api.forms("everything").ref(ref(api)).pageSize(1).submit().map(_.results.headOption)
    } yield {
      checkSlug(maybeDocument, slug, context) {
        case Left(newSlug)   => MovedPermanently(routes.Application.detail(id, newSlug).url)
        case Right(document) => Ok(views.html.detail(document)(request, context))
      }
    }
  }

  // -- Basic Search
  def search(q: Option[String], page: Int) = Action.async { implicit request =>
    for {
      api <- fetchApi
      response <- api.forms("everything").query(Predicate.fulltext("document", q.getOrElse(""))).ref(ref(api)).pageSize(10).page(page).submit()
    } yield {
      Ok(views.html.search(q, response)(request, ctx(api)))
    }
  }

  // -- Preview Action
  def preview(token: String) = Action.async { implicit req =>
    for {
      api <- fetchApi
      context = ctx(api)
      redirectUrl <- api.previewSession(token, context.linkResolver, routes.Application.index().url)
    } yield {
      Redirect(redirectUrl).withCookies(Cookie(Prismic.previewCookie, token, path = "/", maxAge = Some(30 * 60 * 1000), httpOnly = false))
    }
  }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  def checkSlug(document: Option[Document], slug: String, ctx: Context)(callback: Either[String, Document] => Result) =
    document.collect {
      case document if document.slug == slug         => callback(Right(document))
      case document if document.slugs.contains(slug) => callback(Left(document.slug))
    }.getOrElse {
      Results.NotFound(views.html.pageNotFound())
    }
}
