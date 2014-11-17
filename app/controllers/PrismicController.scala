package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * This trait provides an "Action builder" that help to create actions that will query
 * a prismic.io repository.
 *
 * Controllers accessing Prismic.io documents should extend this trait.
 */
trait PrismicController {
  self: Controller =>

  // -- Alternative action builder for the default body parser
  def PrismicAction(block: PrismicHelper.Request[AnyContent] => Future[Result]): Action[AnyContent] =
    bodyAction(parse.anyContent)(block)

  // -- Action builder
  def bodyAction[A](bodyParser: BodyParser[A])(block: PrismicHelper.Request[A] => Future[Result]) =
    Action.async(bodyParser) { implicit request =>
    {
      for {
        ctx <- PrismicHelper.buildContext()
        result <- block(PrismicHelper.Request(request, ctx))
      } yield result
    }
    }

}


