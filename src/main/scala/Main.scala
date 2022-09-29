package com.applaudostudios.fcastro.hp_project

import actors.{JsonLoaderActor, StoreActor}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("Y-AkkaSystem")
    val store = system.actorOf(Props(new StoreActor()), "StoreActor")
    val loader: ActorRef = system.actorOf(JsonLoaderActor.props(store))
    val router = Router(store, loader)
    val source = Http().newServerAt("localhost", 8080)
    source.bind(router.routes)
  }
}
