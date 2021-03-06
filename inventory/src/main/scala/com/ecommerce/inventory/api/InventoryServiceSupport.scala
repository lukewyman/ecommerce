package com.ecommerce.inventory.api

import akka.actor.{ActorSystem, ActorRef}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.Future

/**
  * Created by lukewyman on 12/28/16.
  */
trait InventoryServiceSupport extends RequestTimeOut {

  def start(inventoryItems: ActorRef)(implicit system: ActorSystem): Unit = {
    val config = system.settings.config
    val settings = Settings(system)
    val host = settings.http.host
    val port = settings.http.port

    implicit val ec = system.dispatcher

    val api = new InventoryService(inventoryItems, system, requestTimeout(config)).routes

    implicit val materializer = ActorMaterializer()
    val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(api, host, port)

    val log =  Logging(system.eventStream, "inventoryitems")
    bindingFuture.map { serverBinding =>
      log.info(s"Inventory API bound to ${serverBinding.localAddress} ")
    }.onFailure {
      case ex: Exception =>
        log.error(ex, "Failed to bind to {}:{}!", host, port)
        system.terminate()
    }
  }
}

trait RequestTimeOut {
  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
