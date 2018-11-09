package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.libs.json.JsValue
import play.api.mvc._
import utils.Twitter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class Search @Inject() (cc: ControllerComponents) (cache: SyncCacheApi, twitter: Twitter,failFast: FailFast, waitForNoReason: WaitForNoReason) extends AbstractController(cc) {

  def tweets(query: String) = (failFast andThen waitForNoReason) async {
    cache.get[JsValue](query).fold {
      try {
        twitter.bearerToken.flatMap { bearerToken =>
          twitter.fetchTweets(bearerToken, query).map { response =>
            cache.set(query, response.json, 1.hour)
            Ok(response.json)
          }
        }
      } catch {
        case illegalArgumentException: IllegalArgumentException =>
          Logger.error("Twitter Bearer Token is missing", illegalArgumentException)
          Future(InternalServerError("Error talking to Twitter"))
      }
    } { result =>
      Future.successful(Ok(result))
    }
  }

}
