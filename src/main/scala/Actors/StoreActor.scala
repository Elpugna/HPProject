package com.applaudostudios.fcastro.HPProject
package Actors
import Actors.StoreActor._
import Data.{Customer, Product, Review}

import akka.actor.{ActorContext, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Failure


object StoreActor{
  //Commands
  case class  CreateProduct(product: Product)
  case class  ReadProduct(id: String)
  case class  ReadProductReviews(id: String)
  case object ReadProducts
  case class  UpdateProduct(id:String,product: Product)
  case class  RemoveProduct(id:String)

  case class  CreateCustomer(customer: Customer)
  case class  ReadCustomer(id: Long)
  case class  ReadCustomerReviewedProducts(id: Long)
  case object ReadCustomers
  case class  UpdateCustomer(id:Long,customer: Customer)
  case class  RemoveCustomer(id:Long)

  case class  CreateReview(review:Review)
  case class  ReadReviewList(reviews:Seq[String])
  case class  ReadReviewedProducts(reviews:Seq[String])
  case class  ReadReview(id: String)
  case object ReadReviews
  case class  UpdateReview(id:String,review: Review)
  case class  RemoveReview(id:String)

  case object GetStats

  case object MassLoadStarted
  case object MassLoadOver
  case class MassLoadFailure(t:Throwable)

  //Events
  case class ProductCreated(product: Product)
  case class ProductRemoved(id: String)

  case class CustomerCreated(customer: Customer)
  case class CustomerRemoved(id: Long)

  case class ReviewCreated(review: Review)

  case class StoreState(var opCount:Long,
                        var customers:mutable.Map[Long,(ActorRef,Boolean)],
                        var products: mutable.Map[String,(ActorRef,Boolean)],
                        var reviews: mutable.Map[String,(ActorRef,Boolean)]){
    def toSnap:StoreSnapState = StoreSnapState(
      opCount,
      customers.map(c=>(c._1,c._2._2)),
      products.map(p=>(p._1,p._2._2)),
      reviews.map(p=>(p._1,p._2._2))
    )
  }
  case class StoreSnapState(opCount:Long,
                            customers:mutable.Map[Long,Boolean],
                            products: mutable.Map[String,Boolean],
                             reviews: mutable.Map[String,Boolean]){
    def fromSnap(implicit context:ActorContext,store:ActorRef):StoreState =
      StoreState(opCount,
        customers.map(c=>(c._1,(context.actorOf(CustomerActor.props(c._1,store),s"customer-actor-${c._1}"),c._2))),
        products.map(c=>(c._1,(context.actorOf(ProductActor.props(c._1,store),s"product-actor-${c._1}"),c._2))),
        reviews.map(c=>(c._1,(context.actorOf(ReviewActor.props(c._1,store),s"reviews-actor-${c._1}"),c._2)))
      )
  }
}


case class NotFoundException(message:String) extends RuntimeException
case class AlreadyExistsException(message:String) extends RuntimeException


class StoreActor extends PersistentActor with ActorLogging{
  override def receiveRecover: Receive = {
    case ProductCreated(product)=>
      val actor = context.actorOf(ProductActor.props(product.id,self),s"product-actor-${product.id}")
      state.products.addOne(product.id,(actor,true))
      state.opCount+=1
    case ProductRemoved(id) =>
      state.products.put(id,state.products(id).copy(_2=false))
      state.opCount+=1
    case CustomerCreated(customer)=>
      val actor = context.actorOf(CustomerActor.props(customer.id,self),s"customer-actor-${customer.id}")
      state.customers.addOne(customer.id,(actor,true))
      state.opCount+=1
    case CustomerRemoved(id) =>
      state.customers.put(id,state.customers(id).copy(_2=false))
      state.opCount+=1
    case ReviewCreated(review)=>
      val actor = context.actorOf(ReviewActor.props(review.id,self),s"review-actor-${review.id}")
      state.reviews.addOne(review.id,(actor,true))
      state.opCount+=1
    case ReviewRemoved(id) =>
      state.reviews.put(id,state.reviews(id).copy(_2=false))
      state.opCount+=1
    case SnapshotOffer(meta,snapshot:StoreSnapState) =>
      log.debug(s"Accepted snap $meta with:\n"
        +s"${snapshot.products.size} Products\n"
        +s"${snapshot.customers.size} Customers\n"
      +s"${snapshot.reviews.size} Reviews")
      state = snapshot.fromSnap
  }
  var state:StoreState =StoreState(0,
    mutable.HashMap[Long,(ActorRef,Boolean)](),
    mutable.HashMap[String,(ActorRef,Boolean)](),
    mutable.HashMap[String,(ActorRef,Boolean)]()
  )
  def testAndSnap(): Unit = {
    state.opCount+=1
    if (state.opCount%20 ==0) saveSnapshot(state.toSnap)
  }
  def customerIsAvailable(id:Long): Boolean = state.customers.contains(id) && state.customers(id)._2
  def productIsAvailable(id:String): Boolean = state.products.contains(id) && state.products(id)._2
  def reviewIsAvailable(id:String): Boolean = state.reviews.contains(id) && state.reviews(id)._2

  override def receiveCommand: Receive = {
    case MassLoadStarted =>
      log.info("Mass Load of data from JSON started")
      sender() ! true
    case MassLoadFailure(throwable:Throwable) => log.info("Mass Load of data from JSON failed due to: "+throwable.getLocalizedMessage)
    case MassLoadOver => log.info("Mass Load of data from JSON Over")

    case CreateProduct(product) if !state.products.contains(product.id) =>
        persist(ProductCreated(product)){ created=>
          log.info(s"Creating new Review: ${product.id}")
          val actor = context.actorOf(ProductActor.props(product.id,self),s"product-actor-${product.id}")
          state.products.addOne(product.id,(actor,true))
          actor forward ProductActor.Create(product)
          testAndSnap()
        }
    case CreateProduct(product) =>
      log.info(s"Product ${product.id} already existed")
      sender() ! Failure(AlreadyExistsException(s"Product ${product.id} already existed"))
    case ReadProduct(id) if productIsAvailable(id) =>
      state.products(id)._1 forward ProductActor.Read
    case ReadProductReviews(id) if productIsAvailable(id) =>
      state.products(id)._1 forward ProductActor.ReadReviews
    case ReadProducts =>
      sender() ! Future.sequence(state.products.values.filter(_._2).map(_._1 ? ProductActor.Read))
    case UpdateProduct(id,product) if productIsAvailable(id)=>
      state.products(id)._1 forward ProductActor.Update(product)
    case RemoveProduct(id) if productIsAvailable(id) => persist(ProductRemoved(id)) { remove=>
        val rem = state.products.remove(id).get
        state.products.put(id,rem.copy(_2=false))
        rem._1 forward ProductActor.Read
        testAndSnap()
      }
    case UpdateProduct(_,_) | RemoveProduct(_) | ReadProduct(_) | ReadProductReviews(_) =>
      sender() ! Failure(NotFoundException("The requested product was not available or did not exist"))

    case CreateCustomer(customer) if !state.customers.contains(customer.id) =>
        persist(CustomerCreated(customer)){ cC=>
          log.info(s"Creating new Customer: ${customer.id}")
          val actor =context.actorOf(CustomerActor.props(customer.id,self))
          state.customers.addOne(customer.id,(actor,true))
          actor forward CustomerActor.Create(customer)
          testAndSnap()
        }
    case CreateCustomer(customer) =>
      log.info(s"Customer ${customer.id} already existed")
      sender() ! Failure(AlreadyExistsException(s"Customer ${customer.id} already existed"))
    case ReadCustomer(id) if customerIsAvailable(id) =>
      state.customers(id)._1 forward CustomerActor.Read
    case ReadCustomerReviewedProducts(id) if customerIsAvailable(id)=>
      log.info(s"Getting Reviews from Customer $id")
      state.customers(id)._1 forward CustomerActor.ReadReviews
    case ReadReviewedProducts(reviews) =>
      log.info(s"Got  ${reviews.size} reviews to get products for")
      val reply= Future.sequence(
        state.reviews.filter(rev=>reviews.contains(rev._1) && rev._2._2).values
        .map(r=>(r._1 ? ReviewActor.Read)
        .map{case r:Review=>
          Await.result(state.products(r.product)._1 ? ProductActor.Read,100 millis) match{
            case p:Product => (r,p)
          }})
      ) //TODO: there's got to be a better way to do this...
      log.info(s"replied with ${reply.value.getOrElse(None).toString} to ${sender.path}")
      sender() ! reply
    case ReadCustomers =>
      sender() ! Future.sequence(state.customers.values.filter(_._2).map(_._1 ? CustomerActor.Read))
    case RemoveCustomer(id) if customerIsAvailable(id) => persist(CustomerRemoved(id)) { remove=>
      val rem = state.customers.remove(id).get
      state.customers.put(id,rem.copy(_2=false))
      rem._1 forward CustomerActor.Read
      testAndSnap()
    }
    case UpdateCustomer(id,customer) if customerIsAvailable(id) =>
      state.customers(id)._1 forward CustomerActor.Update(customer)

    case UpdateCustomer(_,_) | RemoveCustomer(_) | ReadCustomer(_) =>
      sender() ! Failure(NotFoundException("The requested customer was not available or did not exist"))
    case CreateReview(review) if
      !state.reviews.contains(review.id)
      && productIsAvailable(review.product)
      && customerIsAvailable(review.customer)=>
      persist(ReviewCreated(review)){ rC=>
        log.info(s"Creating new Review: ${review.id}")
        val actor =context.actorOf(ReviewActor.props(review.id,self))
        actor forward ReviewActor.Create(review)
        state.reviews.addOne(review.id,(actor,true))
        state.products(review.product)._1 ! ProductActor.AddReview(review.id)
        state.customers(review.customer)._1 ! CustomerActor.AddReview(review.id)
        testAndSnap()
      }
    case CreateReview(review) if state.reviews.contains(review.id) =>
      log.info(s"Review ${review.id} already existed")
      sender() ! Failure(AlreadyExistsException(s"Review ${review.id} already existed"))
    case CreateReview(review) =>
      val c= if (!customerIsAvailable(review.customer)) "The customer was not available or did not exist" else ""
      val p= if (!productIsAvailable(review.product)) "The product was not available or did not exist" else ""
      sender() ! Failure(AlreadyExistsException(List(c,p).mkString(". ")))
    case ReadReview(id) if reviewIsAvailable(id) =>
      state.reviews(id)._1 forward ReviewActor.Read
    case ReadReviews =>
      sender() ! Future.sequence(state.reviews.values.filter(_._2).map(_._1 ? ReviewActor.Read))
    case ReadReviewList(reviews) =>
      sender() ! Future.sequence(state.reviews.filter(rev=>reviews.contains(rev._1) && rev._2._2).values.map(_._1 ? ReviewActor.Read))
    case RemoveReview(id) if reviewIsAvailable(id) => persist(ReviewRemoved(id)) { remove=>
      val rem = state.reviews.remove(id).get
      state.reviews.put(id,rem.copy(_2=false))
      rem._1 forward ReviewActor.Read
      testAndSnap()
    }
    case UpdateReview(id,review) if reviewIsAvailable(id) =>
      state.reviews(id)._1 forward ReviewActor.Update(review)

    case UpdateReview(_,_) | RemoveReview(_) | ReadReview(_) =>
      sender() ! Failure(NotFoundException("The requested review was not available or did not exist"))
    case GetStats =>
      sender() ! s"Store has:\n${state.products.size} Products\n${state.customers.size} Customers\n${state.reviews.size} Reviews"

    case SaveSnapshotSuccess(meta) => _
    case SaveSnapshotFailure(cause,metadata) => log.error(s"Snapshot Save Failure for $metadata, due to $cause.")
    case any:Any =>
      val msg = s"Did not find match for ${any.toString}"
      log.error(msg)
      sender() ! Failure(NotFoundException(msg))
  }

  override def persistenceId: String = "my-store-central-actor"

}
