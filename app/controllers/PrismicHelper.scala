package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import Play.current

import io.prismic._

/**
 * This Prismic object contains several helpers that make it easier
 * to build your application using both Prismic.io and Play:
 *
 * It reads some configuration from the application.conf file.
 *
 * The debug and error messages emitted by the Scala Kit are redirected
 * to the Play application Logger.
 */
object PrismicHelper {

  // -- Define the key name to use for storing the Prismic.io access token into the Play session
  private val ACCESS_TOKEN = "ACCESS_TOKEN"

  // -- Cache to use (default to keep 200 JSON responses in a LRU cache)
  private val Cache = BuiltInCache(200)

  // -- Write debug and error messages to the Play `prismic` logger (check the configuration in application.conf)
  private val Logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic").debug(message)
    case 'ERROR => play.api.Logger("prismic").error(message)
    case _      => play.api.Logger("prismic").info(message)
  }

  // Helper method to read the Play application configuration
  private def config(key: String) =
    Play.configuration.getString(key).getOrElse(sys.error(s"Missing configuration [$key]"))

  def endpoint = config("prismic.api")

  // -- Define a `Prismic request` that contain both the original request and the Prismic call context
  case class Request[A](request: play.api.mvc.Request[A], ctx: Context) extends WrappedRequest(request)

  // -- A Prismic context that help to keep the reference to useful primisc.io contextual data
  case class Context(api: Api, ref: String, accessToken: Option[String], linkResolver: DocumentLinkResolver) {
    def hasPrivilegedAccess = accessToken.isDefined
  }

  object Context {
    implicit def fromRequest(implicit req: Request[_]): Context = req.ctx
  }

  // -- Build a Prismic context
  def buildContext()(implicit request: RequestHeader) = {
    val token = request.session.get(ACCESS_TOKEN).orElse(Play.configuration.getString("prismic.token"))
    apiHome(token) map { api =>
      val experimentRef: Option[String] = request.cookies.get(Prismic.experimentsCookie).map(_.value).flatMap(api.experiments.refFromCookie)
      val previewRef: Option[String] = request.cookies.get(Prismic.previewCookie).map(_.value)
      val ref: String = previewRef.orElse(experimentRef).getOrElse(api.master.ref)
      Context(api, ref, token, Application.linkResolver(api)(request))
    }
  }

  // -- Retrieve the Prismic Context from a request handled by an built using Prismic.action
  def ctx(implicit req: Request[_]) = req.ctx

  // -- Fetch the API entry document
  def apiHome(token: Option[String] = None) =
    Api.get(endpoint, accessToken = token, cache = Cache, logger = Logger)

  // -- Helper: Retrieve a single document by Id
  def getDocument(id: String)(implicit ctx: PrismicHelper.Context): Future[Option[Document]] =
    ctx.api.forms("everything")
      .query(Predicate.at("document.id", id))
      .ref(ctx.ref).submit() map (_.results.headOption)

  // -- Helper: Retrieve several documents by Id
  def getDocuments(ids: String*)(implicit ctx: PrismicHelper.Context): Future[Seq[Document]] =
    ids match {
      case Nil => Future.successful(Nil)
      case ids => ctx.api.forms("everything")
        .query(Predicate.any("document.id", ids))
        .ref(ctx.ref).submit() map (_.results)
    }

  // -- Helper: Retrieve a single document from its bookmark
  def getBookmark(bookmark: String)(implicit ctx: PrismicHelper.Context): Future[Option[Document]] =
    ctx.api.bookmarks.get(bookmark).map(id => getDocument(id)).getOrElse(Future.successful(None))

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  def checkSlug(document: Option[Document], slug: String)(callback: Either[String, Document] => Result)(implicit r: PrismicHelper.Request[_]) =
    document.collect {
      case document if document.slug == slug         => callback(Right(document))
      case document if document.slugs.contains(slug) => callback(Left(document.slug))
    }.getOrElse {
      Results.NotFound(views.html.pageNotFound())
    }

}
