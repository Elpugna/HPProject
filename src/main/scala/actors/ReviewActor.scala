package com.applaudostudios.fcastro.hp_project
package actors

import actors.ReviewActor._
import data.Review

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}

object ReviewActor {
  def props(id: String, store: ActorRef): Props = Props(
    new ReviewActor(id, store)
  )

  //Commands
  case class Create(product: Review)

  case class Update(product: Review)

  case class SetCustomer(customer: Long)

  case class SetProduct(product: String)

  //Events
  case class ReviewCreated(initial: Review) extends MySerializable

  case class ReviewUpdated(review: Review) extends MySerializable

  case class CustomerSet(id: Long) extends MySerializable

  case class ProductSet(id: String) extends MySerializable

  case class ReviewState(review: Review) extends MySerializable

}

class ReviewActor(id: String, store: ActorRef)
    extends PersistentActor
    with ActorLogging {
  var state: Review = Review(
    id = id,
    helpful = 0,
    votes = 0,
    vine = Some(false),
    verified = Some(false)
  )

  override def receiveRecover: Receive = {
    case ReviewUpdated(updated) => applyUpdates(updated)
    case ReviewCreated(initial) => state = initial
    case SetCustomer(customer) =>
      state = state.copy(customer = customer)
    case SetProduct(product) =>
      state = state.copy(product = product)
    case SnapshotOffer(_, snap: Review) => state = snap
  }

  override def receiveCommand: Receive = {
    case Read =>
      sender() ! state
    case Create(initial) =>
      persist(ReviewCreated(initial)) { createEvent =>
        state = createEvent.initial
        sender() ! state
      }
    case Update(updated) =>
      persist(ReviewUpdated(updated)) { updateEvent =>
        applyUpdates(updateEvent.review)
        sender() ! state
        testAndSnap()
      }
    case SetCustomer(id) =>
      persist(CustomerSet(id)) { _ =>
        state = state.copy(customer = id)
        sender() ! state
        testAndSnap()
      }
    case SetProduct(id) =>
      persist(ProductSet(id)) { _ =>
        log.info(s"Set product to $id")
        state = state.copy(product = id)
        sender() ! state
        testAndSnap()
      }
  }

  def applyUpdates(updated: Review): Unit = {
    state = Review(
      id = id,
      region = if (!updated.region.isBlank) updated.region else state.region,
      title = if (!updated.title.isBlank) updated.title else state.title,
      customer = state.customer,
      product = state.product,
      body = if (!updated.body.isBlank) updated.body else state.body,
      rating = if (updated.rating != 0) updated.rating else state.rating,
      helpful = if (updated.helpful >= 0) updated.helpful else state.helpful,
      votes = if (updated.votes >= 0) updated.votes else state.votes,
      date =
        if (updated.date.equals(DateTime.apply(0))) updated.date
        else state.date,
      vine = updated.vine match {
        case None       => state.vine;
        case Some(bool) => Some(bool)
      },
      verified = updated.verified match {
        case None       => state.verified;
        case Some(bool) => Some(bool)
      }
    )
  }

  def testAndSnap(): Unit = {
    if (lastSequenceNr % 100 == 0 && lastSequenceNr > 0) saveSnapshot(state)
  }

  override def persistenceId: String = s"review-actor-$id"
}
