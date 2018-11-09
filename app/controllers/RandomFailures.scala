package controllers

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Random, Try}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class FailFast @Inject() (cc: ControllerComponents) extends ActionBuilder[Request,AnyContent] with ActionFilter[Request] with Results {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    // fail about once in every 10 times
    if (Random.nextInt(10) == 0) {
      Future.successful(Some(InternalServerError))
    }
    else {
      Future.successful(None)
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
}

class WaitForNoReason @Inject() (actorSystem: ActorSystem, cc: ControllerComponents) extends ActionBuilder[Request,AnyContent] with ActionFilter[Request] with Results {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    // fail about once in every 10 times
    if (Random.nextInt(10) == 0) {
      val p = Promise[Option[Result]]()
      actorSystem.scheduler.scheduleOnce(25.seconds) {
        p.complete(Try(Some(RequestTimeout)))
      }
      p.future
    }
    else {
      Future.successful(None)
    }
  }
  override protected def executionContext: ExecutionContext = cc.executionContext
  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
}