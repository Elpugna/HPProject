package com.applaudostudios.fcastro.hp_project

import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

package object actors {
  implicit val timeout: Timeout = 20 seconds
}

trait MySerializable

//common Commands
case class AddReview(id: String) extends MySerializable

case class RemoveReview(id: String) extends MySerializable

case object ReadReviewIds

case object Read

case object End

//common Events
case class ReviewAdded(id: String) extends MySerializable

case class ReviewRemoved(id: String) extends MySerializable

//common Exceptions
case class NotFoundException(message: String) extends RuntimeException(message)

class AlreadyExistsException(message: String)
    extends RuntimeException(message)
