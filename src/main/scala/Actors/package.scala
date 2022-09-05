package com.applaudostudios.fcastro.HPProject

import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

package object Actors {
  implicit val timeout:Timeout=1500 millis
}


//common Events
case class  ReviewAdded(id:String)
case class  ReviewRemoved(id:String)