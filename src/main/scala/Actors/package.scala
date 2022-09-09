package com.applaudostudios.fcastro.HPProject

import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

package object Actors {
  implicit val timeout:Timeout=5 seconds
}

trait MySerializable

//common Commands
case class  AddReview(id:String) extends MySerializable
case class  RemoveReview(id:String) extends MySerializable
case object ReadReviewIds

//common Events
case class  ReviewAdded(id:String) extends MySerializable
case class  ReviewRemoved(id:String) extends MySerializable

//common Exceptions
case class NotFoundException(message:String) extends RuntimeException(message)
case class AlreadyExistsException(message:String) extends RuntimeException(message)