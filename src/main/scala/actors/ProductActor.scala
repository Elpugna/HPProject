package com.applaudostudios.fcastro.hp_project
package actors

import actors.ProductActor._
import actors.StoreActor.{ReadReviewIdList, ReadReviewList}
import data._

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.collection.mutable
import scala.util.Failure

object ProductActor {
  def props(id: String, store: ActorRef): Props = Props(
    new ProductActor(id, store)
  )

  //Commands
  case class Create(product: data.Product)

  case class Update(product: data.Product)

  //Events
  case class ProductCreated(initial: data.Product) extends MySerializable

  case class ProductUpdated(product: data.Product) extends MySerializable

  case class ProductState(
      var product: Product,
      var reviews: mutable.Set[String]
  ) extends MySerializable

  case object ReadReviews

}

case class ProductActor(id: String, store: ActorRef)
    extends PersistentActor
    with ActorLogging {
  var state: ProductState = ProductState(
    Product(id),
    mutable.LinkedHashSet[String]()
  )

  override def receiveRecover: Receive = {
    case ProductUpdated(updated)              => applyUpdates(updated)
    case ProductCreated(initial)              => state.product = initial
    case ReviewAdded(review)                  => state.reviews.addOne(review)
    case ReviewRemoved(review)                => state.reviews.remove(review)
    case SnapshotOffer(_, snap: ProductState) => state = snap
  }

  def applyUpdates(updated: Product): Unit = {
    state.product = Product(
      id,
      if (!updated.title.isBlank) updated.title else state.product.title,
      if (!updated.category.isBlank) updated.category
      else state.product.category
    )
  }

  override def receiveCommand: Receive = {
    case Read => sender() ! state.product
    case ReadReviewIds =>
      store forward ReadReviewIdList(state.reviews.toList)
    case ProductActor.ReadReviews =>
      store forward ReadReviewList(state.reviews.toList)
    case Create(initial) =>
      persist(ProductCreated(initial)) { createEvent =>
        state.product = createEvent.initial
        sender() ! state.product
      }
    case Update(updated) =>
      persist(ProductUpdated(updated)) { updateEvent =>
        applyUpdates(updateEvent.product)
        sender() ! state.product
        testAndSnap()
      }
    case AddReview(id) =>
      persist(ReviewAdded(id)) { _ =>
        state.reviews.addOne(id)
        testAndSnap()
      }
    case RemoveReview(id) if state.reviews.contains(id) =>
      persist(ReviewRemoved(id)) { _ =>
        testAndSnap()
        sender() ! state.reviews.remove(id)
      }
    case RemoveReview(id) =>
      sender() ! Failure(
        NotFoundException(
          s"Product ${state.product.id} did not have Review $id"
        )
      )
  }

  def testAndSnap(): Unit = {
    if (lastSequenceNr % 100 == 0 && lastSequenceNr > 0) saveSnapshot(state)
  }

  override def persistenceId: String = s"product-actor-$id"
}
