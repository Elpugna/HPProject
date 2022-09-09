package com.applaudostudios.fcastro.HPProject
package Actors
import Actors.StoreActor._
import Data.{Customer, Product, Review}

import akka.actor.{ActorContext, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.persistence._

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
  case class  ReadProductReviewIds(id: String)
  case object ReadProducts
  case class  ReadProductsPaged(page:Int,size:Int=50)
  case class  UpdateProduct(id:String,product: Product)
  case class  RemoveProduct(id:String)

  case class  CreateCustomer(customer: Customer)
  case class  ReadCustomer(id: Long)
  case class  ReadCustomerReviewedProducts(id: Long)
  case class  ReadCustomerReviewIds(id: Long)
  case object ReadCustomers
  case class  ReadCustomersPaged(page:Int,size:Int=50)
  case class  UpdateCustomer(id:Long,customer: Customer)
  case class  RemoveCustomer(id:Long)

  case class  CreateReview(review:Review)
  case class  ReadReviewList(reviews:Seq[String])
  case class  ReadReviewIdList(reviews:Seq[String])
  case class  ReadReviewedProducts(reviews:Seq[String])
  case class  ReadReview(id: String)
  case object ReadReviews
  case class  ReadReviewsPaged(page:Int,size:Int=50)
  case class  UpdateReview(id:String,review: Review)
  case class  UpdateReviewProduct(review: String,product:String)
  case class  UpdateReviewCustomer(review: String,customer: Long)

  case object GetStats

  case object MassLoadStarted
  case object MassLoadOver
  case class  MassLoadFailure(t:Throwable)

  //Events
  case class ProductCreated(product: Product) extends MySerializable
  case class ProductRemoved(id: String) extends MySerializable

  case class CustomerCreated(customer: Customer) extends MySerializable
  case class CustomerRemoved(id: Long) extends MySerializable

  case class ReviewCreated(review: Review) extends MySerializable
  case class ReviewProductUpdated(review: String,product:String) extends MySerializable
  case class ReviewCustomerUpdated(review: String,customer: Long) extends MySerializable
  case class StoreState(var opCount:Long,
                        customers:mutable.LongMap[(ActorRef,Boolean)],
                        products: mutable.AnyRefMap[String,(ActorRef,Boolean)],
                        reviews: mutable.AnyRefMap[String,(ActorRef,Boolean)]){
    def toSnap:StoreSnapState = StoreSnapState(
      opCount,
      customers.map(c=>(c._1,c._2._2)),
      products.map(p=>(p._1,p._2._2)),
      reviews.map(p=>(p._1,p._2._2))
    )
  }
  case class StoreSnapState(opCount:Long,
                            customers:mutable.LongMap[Boolean],
                            products: mutable.AnyRefMap[String,Boolean],
                             reviews: mutable.AnyRefMap[String,Boolean]){
    def fromSnap(implicit context:ActorContext,store:ActorRef):StoreState =
      StoreState(opCount,
        customers.map(c=>(c._1,(context.actorOf(CustomerActor.props(c._1,store),s"customer-actor-${c._1}"),c._2))),
        products.map(c=>(c._1,(context.actorOf(ProductActor.props(c._1,store),s"product-actor-${c._1}"),c._2))),
        reviews.map(c=>(c._1,(context.actorOf(ReviewActor.props(c._1,store),s"reviews-actor-${c._1}"),c._2)))
      )
  }
}





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
    mutable.LongMap[(ActorRef,Boolean)](),
    mutable.AnyRefMap[String,(ActorRef,Boolean)](),
    mutable.AnyRefMap[String,(ActorRef,Boolean)]()
  )
  def testAndSnap(): Unit = {
    state.opCount+=1
    if (state.opCount%100 ==0) saveSnapshot(state.toSnap)
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
        persist(ProductCreated(product)){ _ =>
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
    case ReadProductReviewIds(id) if productIsAvailable(id) =>
      state.products(id)._1 forward ReadReviewIds
    case ReadProducts =>
      sender() ! Future.sequence(state.products.values.filter(_._2).map(_._1 ? ProductActor.Read))
    case ReadProductsPaged(i,l) =>
      val filtered=state.products.values.filter(_._2)
      sender() ! (Future.sequence(filtered.slice(i*l,(i+1)*l).map(_._1 ? ProductActor.Read)),filtered.size)
    case UpdateProduct(id,product) if productIsAvailable(id)=>
      state.products(id)._1 forward ProductActor.Update(product)
    case RemoveProduct(id) if productIsAvailable(id) => persist(ProductRemoved(id)) { _ =>
        val rem = state.products.remove(id).get
        state.products.put(id,rem.copy(_2=false))
        rem._1 forward ProductActor.Read
        testAndSnap()
      }
    case UpdateProduct(_,_) | RemoveProduct(_) | ReadProduct(_) | ReadProductReviews(_) | ReadProductReviewIds(_) =>
      sender() ! Failure(NotFoundException("The requested product was not available or did not exist"))

    case CreateCustomer(customer) if !state.customers.contains(customer.id) =>
        persist(CustomerCreated(customer)){ _ =>
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
      state.customers(id)._1 forward CustomerActor.ReadReviewProducts
    case ReadCustomerReviewIds(id) if customerIsAvailable(id) =>
      state.customers(id)._1 forward ReadReviewIds
    case ReadReviewedProducts(reviews) =>
      log.info(s"Got  ${reviews.size} reviews to get products for")
      val reply= Future.sequence(
        state.reviews.filter(rev=>reviews.contains(rev._1) && rev._2._2).values
        .map(r=>(r._1 ? ReviewActor.Read)
        .map{case r:Review=>
          Await.result(state.products(r.product)._1 ? ProductActor.Read,100 millis) match{
            case p:Product => (r,p)
          }})
      )
      log.info(s"replied with ${reply.value.getOrElse(None).toString} to ${sender.path}")
      sender() ! reply
    case ReadCustomers =>
      sender() ! Future.sequence(state.customers.values.filter(_._2).map(_._1 ? CustomerActor.Read))
    case ReadCustomersPaged(i,l) =>
      val filtered=state.customers.values.filter(_._2)
      sender() ! (Future.sequence(filtered.slice(i*l,(i+1)*l).map(_._1 ? CustomerActor.Read)),filtered.size)
    case RemoveCustomer(id) if customerIsAvailable(id) => persist(CustomerRemoved(id)) { _ =>
      val rem = state.customers.remove(id).get
      state.customers.put(id,rem.copy(_2=false))
      rem._1 forward CustomerActor.Read
      testAndSnap()
    }
    case UpdateCustomer(id,customer) if customerIsAvailable(id) =>
      state.customers(id)._1 forward CustomerActor.Update(customer)

    case UpdateCustomer(_,_) | RemoveCustomer(_) | ReadCustomer(_) | ReadCustomerReviewIds(_) |  ReadCustomerReviewedProducts(_) =>
      sender() ! Failure(NotFoundException("The requested customer was not available or did not exist"))
    case CreateReview(review) if
      !state.reviews.contains(review.id)
      && productIsAvailable(review.product)
      && customerIsAvailable(review.customer)=>
      persist(ReviewCreated(review)){ _ =>
        log.info(s"Creating new Review: ${review.id}")
        val actor =context.actorOf(ReviewActor.props(review.id,self))
        actor forward ReviewActor.Create(review)
        state.reviews.addOne(review.id,(actor,true))
        state.products(review.product)._1 ! AddReview(review.id)
        state.customers(review.customer)._1 ! AddReview(review.id)
        testAndSnap()
      }
    case CreateReview(review) if state.reviews.contains(review.id) =>
      log.info(s"Review ${review.id} already existed")
      sender() ! Failure(AlreadyExistsException(s"Review ${review.id} already existed"))
    case CreateReview(review) =>
      val c= if (!customerIsAvailable(review.customer)) "The customer was not available or did not exist" else ""
      val p= if (!productIsAvailable(review.product)) "The product was not available or did not exist" else ""
      sender() ! Failure(NotFoundException(List(c,p).mkString(". ")))
    case ReadReview(id) if reviewIsAvailable(id) =>
      state.reviews(id)._1 forward ReviewActor.Read
    case ReadReviews =>
      sender() ! Future.sequence(state.reviews.values.filter(_._2).map(_._1 ? ReviewActor.Read))
    case ReadReviewsPaged(i,l) =>
      val filtered=state.reviews.values.filter(_._2)
      sender() ! (Future.sequence(filtered.slice(i*l,(i+1)*l).map(_._1 ? ReviewActor.Read)),filtered.size)
    case ReadReviewIdList(reviews) =>
      sender() ! reviews.filter(reviewIsAvailable)
    case ReadReviewList(reviews) =>
      sender() ! Future.sequence(state.reviews.filter(rev=>reviews.contains(rev._1) && rev._2._2).values.map(_._1 ? ReviewActor.Read))
    case RemoveReview(id) if reviewIsAvailable(id) => persist(ReviewRemoved(id)) { _ =>
      val rem = state.reviews.remove(id).get
      state.reviews.put(id,rem.copy(_2=false))
      rem._1 forward ReviewActor.Read
      testAndSnap()
    }
    case UpdateReview(id,review) if reviewIsAvailable(id) =>
      state.reviews(id)._1 forward ReviewActor.Update(review)
    case UpdateReviewCustomer(id,customer) if reviewIsAvailable(id) && customerIsAvailable(customer) =>
      val send = sender()
      (state.reviews(id)._1 ? ReviewActor.Read).map({
        case oldReview:Review =>
          (state.customers(oldReview.customer)._1 ? RemoveReview(id)).map{
            case ex@Failure(_:NotFoundException) =>
              send ! ex
            case _ =>
              state.customers(customer)._1 ! AddReview(id)
              send ! Await.result[Review]((state.reviews(id)._1 ? ReviewActor.SetCustomer(customer)).mapTo[Review],timeout.duration)
          }
      })
    case UpdateReviewCustomer(id,customer) =>
      val p= if (!reviewIsAvailable(id)) s"Review #$id was not available or did not exist" else ""
      val c= if (!customerIsAvailable(customer)) s"Customer #$customer was not available or did not exist" else ""
      sender() ! Failure(NotFoundException(List(c,p).mkString(". ")))
    case UpdateReviewProduct(id,product) if reviewIsAvailable(id) && productIsAvailable(product) =>
      val send = sender()
     (state.reviews(id)._1 ? ReviewActor.Read).map({
       case oldReview:Review =>
         (state.products(oldReview.product)._1 ? RemoveReview(id)).map{
           case ex@Failure(_:NotFoundException) =>
             send ! ex
           case _ =>
             state.products(product)._1 ! AddReview(id)
             send ! Await.result[Review]((state.reviews(id)._1 ? ReviewActor.SetProduct(product)).mapTo[Review],timeout.duration)
        }
     })
    case UpdateReviewProduct(id,product) =>
      val p= if (!reviewIsAvailable(id)) s"Review #$id was not available or did not exist" else ""
      val c= if (!productIsAvailable(product)) s"Product #$product was not available or did not exist" else ""
      sender() ! Failure(NotFoundException(List(c,p).mkString(". ")))
    case UpdateReview(_,_) | RemoveReview(_) | ReadReview(_) =>
      sender() ! Failure(NotFoundException("The requested review was not available or did not exist"))
    case GetStats =>
      sender() ! s"Store has:\n${state.products.size} Products\n${state.customers.size} Customers\n${state.reviews.size} Reviews"

    case SaveSnapshotSuccess(meta) => log.debug(s"Saved snapshot: $meta")
    case SaveSnapshotFailure(cause,metadata) => log.error(s"Snapshot Save Failure for $metadata, due to $cause.")
    case any:Any =>
      val msg = s"Did not find match for ${any.toString}"
      log.error(msg)
      sender() ! Failure(NotFoundException(msg))
    case RecoveryCompleted =>
      log.debug("Recovery Completed")
  }

  override def persistenceId: String = "my-store-central-actor"

}

