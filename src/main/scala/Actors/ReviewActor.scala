package com.applaudostudios.fcastro.HPProject

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.DateTime
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.applaudostudios.fcastro.HPProject.Data.Review
import com.applaudostudios.fcastro.HPProject.ReviewActor._

import scala.collection.mutable


object ReviewActor{
  def props(id:String,store:ActorRef): Props = Props(new ReviewActor(id,store))

  //Commands
  case class  Create(product: Review)
  case object Read
  case class  Update(product: Review)
  case class  AddReview(id: String)
  case class  RemoveReview(id: String)
  case object End

  //Events
  case class  ReviewCreated(initial: Review)
  case class  ReviewUpdated(product: Review)

  case class ReviewState(var opCount:Long=0,var review: Review, var reviews: mutable.Set[String]=mutable.LinkedHashSet[String]())
}


class ReviewActor(id:String,store:ActorRef) extends PersistentActor with ActorLogging{
 var state: ReviewState = ReviewState(review=Review(id = id, helpful=0,votes=0, vine = Some(false), verified = Some(false)))

  def testAndSnap(): Unit = {
    state.opCount+=1
    if (state.opCount%20 ==0) saveSnapshot(state)
  }
  def applyUpdates(updated: Review): Unit = {
    state.review=Review(
      id = id,
      region = if(!updated.region.isBlank) updated.region else state.review.region,
      title = if(!updated.title.isBlank) updated.title else state.review.title,
      customer=state.review.customer,
      product=state.review.product,
      body = if(!updated.body.isBlank) updated.body else state.review.body,
      rating = if(updated.rating!=0) updated.rating else state.review.rating,
      helpful = if(updated.helpful>=0) updated.helpful else state.review.helpful,
      votes = if(updated.votes>=0) updated.votes else state.review.votes,
      date = if(updated.date.equals(DateTime.apply(0))) updated.date else state.review.date,
      vine = updated.vine match {case None => state.review.vine; case Some(bool) => Some(bool)},
      verified = updated.verified match {case None => state.review.verified; case Some(bool) => Some(bool)}
    )
  }

  override def receiveRecover: Receive = {
    case ReviewUpdated(updated) => applyUpdates(updated)
    case ReviewCreated(initial) => state.review = initial
    case ReviewAdded(id) => state.reviews.addOne(id)
    case ReviewRemoved(id) => state.reviews.remove(id)
    case SnapshotOffer(meta,snap:ReviewState) => state=snap
  }

  override def receiveCommand: Receive = {
    case Read =>
      sender() ! state.review
    case Create(initial) =>
      persist(ReviewCreated(initial)){ createEvent=>
        state.review=createEvent.initial
        sender() ! (state.review)
      }
    case Update(updated) =>
      persist(ReviewUpdated(updated)){ updateEvent=>
        applyUpdates(updateEvent.product)
        sender() ! (state.review)
        testAndSnap()
      }
    case AddReview(id) =>
      persist(ReviewAdded(id)) { rA=>
        state.reviews.addOne(id)
        testAndSnap()
      }
    case RemoveReview(id) =>
      persist(ReviewRemoved(id)) { rA=>
        state.reviews.remove(id)
        testAndSnap()
      }
    case End =>
      context.stop(self)
  }

  override def persistenceId: String = s"review-actor-$id"
}
