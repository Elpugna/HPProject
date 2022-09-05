package com.applaudostudios.fcastro.HPProject
package Actors

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.applaudostudios.fcastro.HPProject.Actors.ProductActor._
import com.applaudostudios.fcastro.HPProject.Actors.StoreActor.ReadReviewList
import com.applaudostudios.fcastro.HPProject.Data
import com.applaudostudios.fcastro.HPProject.Data.Product

import scala.collection.mutable
import scala.util.Success


object ProductActor{
 def props(id:String,store:ActorRef): Props = Props(new ProductActor(id,store))
  //Commands
  case class  Create(product: Data.Product)
  case object Read
  case object ReadReviews
  case class  Update(product: Data.Product)
  case class  AddReview(id: String)
  case object End

  case class ProductCreated(initial: Data.Product)
  case class ProductUpdated(product: Data.Product)

  case class ProductState(var opCount:Long,var product:Product, var reviews:mutable.Set[String])
}

//Events


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
    case SnapshotOffer(meta,snap:ProductState) => state=snap
  }

  override def receiveCommand: Receive = {
    case Read => sender() ! state.product
    case ProductActor.ReadReviews =>
      store forward  ReadReviewList(state.reviews.toList)
    case Create(initial) => persist(ProductCreated(initial)){ createEvent=>
      state.product=createEvent.initial
      sender() ! (state.product)
    }
    case Update(updated) => persist(ProductUpdated(updated)){ updateEvent=>
      applyUpdates(updateEvent.product)
      sender() ! (state.product)
      testAndSnap()
    }
    case AddReview(id) =>
      persist(ReviewAdded(id)) { rA=>
        state.reviews.addOne(id)
        testAndSnap()
      }
    case End => context.stop(self)
  }

  override def persistenceId: String = s"product-actor-$id"
}
