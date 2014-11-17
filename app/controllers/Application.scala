package controllers

import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import io.prismic._

object Application extends Controller with PrismicController {

  import PrismicHelper._

  // -- Resolve links to documents
  def linkResolver(api: Api)(implicit request: RequestHeader) = DocumentLinkResolver(api) {
    case (Fragment.DocumentLink(id, docType, tags, slug, false), maybeBookmarked) => routes.Application.detail(id, slug).absoluteURL()
    case (link@Fragment.DocumentLink(_, _, _, _, true), _)                        => routes.Application.brokenLink().absoluteURL()
  }

  // -- Page not found
  def PageNotFound(implicit ctx: PrismicHelper.Context) = NotFound(views.html.pageNotFound())

  def brokenLink = PrismicAction { implicit request =>
    Future.successful(PageNotFound)
  }

  // -- Home page
  def index(page: Int) = PrismicAction { implicit request =>
    ctx.api.forms("everything").ref(ctx.ref).pageSize(10).page(page).submit() map { response =>
      Ok(views.html.index(response))
    }
  }

  // -- Document detail
  def detail(id: String, slug: String) = PrismicAction { implicit request =>
    for {
      maybeDocument <- getDocument(id)
    } yield {
      checkSlug(maybeDocument, slug) {
        case Left(newSlug)   => MovedPermanently(routes.Application.detail(id, newSlug).url)
        case Right(document) => Ok(views.html.detail(document))
      }
    }
  }

  // -- Basic Search
  def search(q: Option[String], page: Int) = PrismicAction { implicit request =>
    ctx.api.forms("everything")
      .query(Predicate.fulltext("document", q.getOrElse("")))
      .ref(ctx.ref).pageSize(10).page(page).submit() map { response =>
        Ok(views.html.search(q, response))
      }
  }

  // -- Preview Action
  def preview(token: String) = PrismicAction { implicit req =>
    ctx.api.previewSession(token, ctx.linkResolver, routes.Application.index().url).map { redirectUrl =>
      Redirect(redirectUrl).withCookies(Cookie(Prismic.previewCookie, token, path = "/", maxAge = Some(30 * 60 * 1000), httpOnly = false))
    }
  }

}

