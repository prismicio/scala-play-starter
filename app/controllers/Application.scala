package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import io.prismic._

object Application extends Controller {

  import Prismic._

  // -- Resolve links to documents
  def linkResolver(api: Api, ref: Option[String])(implicit request: RequestHeader) = DocumentLinkResolver(api) { 
    case (Fragment.DocumentLink(id, docType, tags, slug, false), maybeBookmarked) => routes.Application.detail(id, slug, ref).absoluteURL()
    case (link@Fragment.DocumentLink(_, _, _, _, true), _) => routes.Application.brokenLink(ref).absoluteURL()
  }

  // -- Page not found
  def PageNotFound(implicit ctx: Prismic.Context) = NotFound(views.html.pageNotFound())

  def brokenLink(ref: Option[String]) = Prismic.action(ref) { implicit request =>
    Future.successful(PageNotFound)
  }

  // -- Home page
  def index(ref: Option[String]) = Prismic.action(ref) { implicit request =>
    for {
      someDocuments <- ctx.api.forms("everything").ref(ctx.ref).submit()
    } yield {
      Ok(views.html.index(someDocuments))
    }
  }

  // -- Document detail
  def detail(id: String, slug: String, ref: Option[String]) = Prismic.action(ref) { implicit request =>
    for {
      maybeDocument <- getDocument(id)
    } yield {
      checkSlug(maybeDocument, slug) {
        case Left(newSlug) => MovedPermanently(routes.Application.detail(id, newSlug, ref).url)
        case Right(document) => Ok(views.html.detail(document))
      }
    } 
  }

  // -- Basic Search
  def search(q: Option[String], ref: Option[String]) = Prismic.action(ref) { implicit request =>
    for {
      results <- ctx.api.forms("everything").query(s"""[[:d = fulltext(document, "${q.getOrElse("")}")]]""").ref(ctx.ref).submit()
    } yield {
      Ok(views.html.search(q, results))
    }
  }

}
