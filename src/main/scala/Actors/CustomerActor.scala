package com.applaudostudios.fcastro.HPProject
package Actors

import Actors.CustomerActor._
import Actors.StoreActor.{ReadReviewIdList, ReadReviewedProducts}
import Data.Customer

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.collection.mutable
import scala.util.{Failure, Success}

object CustomerActor{
  def props(id:Long,store:ActorRef): Props = Props(new CustomerActor(id,store))

  //Commands
  case class  Create(product: Customer)
  case object Read
  case object ReadReviewProducts
  case class  Update(product: Customer)
  case object End

  //Events
  case class  CustomerCreated(initial: Customer) extends MySerializable
  case class  CustomerUpdated(product: Customer) extends MySerializable

  case class CustomerState(var opCount:Long,var customer: Customer, var reviews:mutable.Set[String]) extends MySerializable
}




class CustomerActor(id:Long,store:ActorRef) extends PersistentActor with ActorLogging{
  var state: CustomerState = CustomerState(
    0,
    Customer(id),
    mutable.LinkedHashSet[String]()
  )

  def testAndSnap(): Unit = {
    state.opCount+=1
    if (state.opCount%20 ==0) saveSnapshot(state)
  }

  def applyUpdates(updated: Customer): Unit = {
    state.customer=Customer(
      id,
      if(!updated.name.isBlank) updated.name else state.customer.name
    )
  }

  override def receiveRecover: Receive = {
    case CustomerUpdated(updated) => applyUpdates(updated)
    case CustomerCreated(initial) => state.customer = initial
    case ReviewAdded(id) => state.reviews.addOne(id)
    case ReviewRemoved(id) => state.reviews.remove(id)
    case SnapshotOffer(_,snap:CustomerState) => state=snap
  }

  override def receiveCommand: Receive = {
    case Read =>
      sender() ! state.customer
    case Create(initial) =>
      persist(CustomerCreated(initial)){ createEvent=>
        state.customer=createEvent.initial
        sender() ! state.customer
      }
    case Update(updated) =>
      persist(CustomerUpdated(updated)){ updateEvent=>
        applyUpdates(updateEvent.product)
        sender() ! state.customer
        testAndSnap()
      }
    case AddReview(id) =>
      persist(ReviewAdded(id)) { _=>
        state.reviews.addOne(id)
        testAndSnap()
      }
    case RemoveReview(id) if state.reviews.contains(id) =>
      persist(ReviewRemoved(id)) { _=>
        state.reviews.remove(id)
        testAndSnap()
        sender() ! Success(state.reviews.remove(id))
      }
    case RemoveReview(id) => Failure(NotFoundException(s"Customer did not have Review $id"))
    case ReadReviewProducts =>
      store forward ReadReviewedProducts(state.reviews.toList)
    case ReadReviewIds =>
      store forward ReadReviewIdList(state.reviews.toList)
    case End =>
      context.stop(self)
  }

  override def persistenceId: String = s"customer-actor-$id"
}
