package com.applaudostudios.fcastro.HPProject
package Actors

import Actors.ProductActor._
import Actors.StoreActor.{ReadReviewIdList, ReadReviewList}
import Data._

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.collection.mutable
import scala.util.Failure


object ProductActor{
 def props(id:String,store:ActorRef): Props = Props(new ProductActor(id,store))

  //Commands
  case class  Create(product: Data.Product)
  case object Read
  case object ReadReviews
  case class  Update(product: Data.Product)
  case object End

  //Events
  case class ProductCreated(initial: Data.Product) extends MySerializable
  case class ProductUpdated(product: Data.Product) extends MySerializable

  case class ProductState(var opCount:Long,var product:Product, var reviews:mutable.Set[String]) extends MySerializable
}




case class ProductActor(id: String,store:ActorRef) extends PersistentActor with ActorLogging{
  var state: ProductState = ProductState(
    0,
    Product(id),
    mutable.LinkedHashSet[String]()
  )

  def testAndSnap(): Unit = {
    state.opCount+=1
    if (state.opCount%20 ==0) saveSnapshot(state)
  }

  def applyUpdates(updated: Product): Unit = {
    state.product=Product(
      id,
      if(!updated.title.isBlank) updated.title else state.product.title,
      if(!updated.category.isBlank) updated.category else state.product.category
    )
  }

  override def receiveRecover: Receive = {
    case ProductUpdated(updated) => applyUpdates(updated)
    case ProductCreated(initial) => state.product = initial
    case ReviewAdded(review) => state.reviews.addOne(review)
    case ReviewRemoved(review) => state.reviews.remove(review)
    case SnapshotOffer(_,snap:ProductState) => state=snap
  }

  override def receiveCommand: Receive = {
    case Read => sender() ! state.product
    case ReadReviewIds =>
      store forward ReadReviewIdList(state.reviews.toList)
    case ProductActor.ReadReviews =>
      store forward  ReadReviewList(state.reviews.toList)
    case Create(initial) => persist(ProductCreated(initial)){ createEvent=>
      state.product=createEvent.initial
      sender() ! state.product
    }
    case Update(updated) => persist(ProductUpdated(updated)){ updateEvent=>
      applyUpdates(updateEvent.product)
      sender() ! state.product
      testAndSnap()
    }
    case AddReview(id) =>
      persist(ReviewAdded(id)) { _=>
        state.reviews.addOne(id)
        testAndSnap()
      }
    case RemoveReview(id) if state.reviews.contains(id) =>
      persist(ReviewRemoved(id)) { _=>
        testAndSnap()
        sender() ! state.reviews.remove(id)
      }
    case RemoveReview(id) => sender() ! Failure(NotFoundException(s"Product did not have Review $id"))
  }

  override def persistenceId: String = s"product-actor-$id"
}
