package com.applaudostudios.fcastro.HPProject

import Actors.JsonLoaderActor.LoadFile
import Actors.StoreActor._
import Actors._
import Data._

import akka.actor.ActorRef
import akka.event.slf4j.Logger
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.pattern.ask

import java.io.FileNotFoundException
import scala.concurrent.Future
import scala.util.{Failure, Success}
case class Router(store:ActorRef,loader:ActorRef) extends SprayJsonSupport with ProductProtocol with CustomerProtocol with ReviewProtocol{


  implicit val exceptionHandler: ExceptionHandler = ExceptionHandler{
    case p:FileNotFoundException => complete(404,"File not found.")
    case p:NotFoundException => complete(404,p.message)
    case p:AlreadyExistsException => complete(400,p.message)
  }

  def routes:Route = pathPrefix("api"){
    handleExceptions(exceptionHandler){

      pathPrefix("load"){
        post{
          entity(as[String]){ path =>
            onSuccess(loader ? LoadFile(path)){
              case Failure(exception) => throw exception
              case any: Any => complete(any.toString)
            }
          }
        }
      }~
        pathPrefix("stats"){
          get{
            entity(as[String]){ path =>
              onSuccess(store ? GetStats){
                case Failure(exception) => throw exception
                case any: Any => complete(any.toString)
              }
            }
          }
        }~
      pathPrefix("product"){
        pathEndOrSingleSlash{
          get{
            onSuccess(store ? ReadProducts){ case seq:Future[Seq[Product]]=>
              onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                case Failure(exception) => throw exception
                case Success(value) => complete(value)
              }
            }
          } ~
          post{
            entity(as[Product]){p=>
              onSuccess(store ? CreateProduct(p)) {
                case Failure(exception) => throw exception
                case value:Product => complete(StatusCodes.Created,value)
              }
            }
          }
        } ~
        (pathPrefix(Segment)  | parameter("id".as[String])){ id:String =>
          pathPrefix("reviews"){
            get {
              onSuccess(store ? ReadProductReviews(id)){ case seq:Future[Seq[Review]]=>
                onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                  case Failure(exception) => throw exception
                  case Success(value) => complete(value)
                }
              }
            }
          }~
            pathPrefix("score"){
              get {
                onSuccess(store ? ReadProductReviews(id)){ case seq:Future[Seq[Review]]=>
                  onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                    case Failure(exception) => throw exception
                    case Success(reviews) =>
                      val aux: Array[(Int,Int)] = Array.fill(5){(0,0)}
                      var total: Double = 0.0d
                      reviews.foreach { review =>
                        val score: Int = review.rating - 1
                        val prev = aux(score)
                        total+=review.rating
                        aux(score) = if(review.verified.getOrElse(false)) prev.copy(_2 = prev._2+1) else prev.copy(_1 = prev._1+1)
                      }
                      complete(Rating(total/reviews.size,aux.toList))
                      }
                  }
                }
            }~
          get{
            onSuccess(store ? ReadProduct(id)) {
              case Failure(exception) => throw exception
              case value:Product => complete(value)
            }
          } ~
          (put | patch){
            entity(as[Product]){ p=>
              onSuccess(store ? UpdateProduct(id,p)) {
                case Failure(exception) => throw exception
                case value:Product => complete(value)
              }
            }
          } ~
          delete{
            onSuccess(store ? RemoveProduct(id)) {
              case Failure(exception) => throw exception
              case value:Product => complete(StatusCodes.NoContent,value)
            }
          }
        }
      } ~
      pathPrefix("customer"){
        pathEndOrSingleSlash{
          get{
            onSuccess(store ? ReadCustomers){ case seq:Future[Seq[Customer]]=>
              onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                case Failure(exception) => throw exception
                case Success(value) => complete(value)
              }
            }
          } ~
            post{
              entity(as[Customer]){customer=>
                onSuccess(store ? CreateCustomer(customer)) {
                  case Failure(exception) => throw exception
                  case value:Customer => complete(StatusCodes.Created,value)
                }
              }
            }
        } ~
          (path(LongNumber) | parameter("id".as[Long])){ id:Long =>
            pathPrefix("reviewed"){
              get {
                onSuccess(store ? ReadCustomerReviewedProducts(id)){
                  case seq:Future[Iterable[(Review,Product)]]=>
                    onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                      case Failure(exception) => throw exception
                      case Success(value:Iterable[(Review,Product)]) =>
                        Logger.root.debug(s"Got: $value")
                        complete(value)
                      case Success(any) => complete(StatusCodes.InternalServerError,any.toString())
                    }
                }
              }
            }~
            get{
              onSuccess(store ? ReadCustomer(id)) {
                case Failure(exception) => throw exception
                case value:Customer => complete(value)
              }
            } ~
              (put | patch){
                entity(as[Customer]){ p=>
                  onSuccess(store ? UpdateCustomer(id,p)) {
                    case Failure(exception) => throw exception
                    case value:Customer => complete(value)
                  }
                }
              } ~
              delete{
                onSuccess(store ? RemoveCustomer(id)) {
                  case Failure(exception) => throw exception
                  case value:Customer => complete(StatusCodes.NoContent,value)
                }
              }
          }
      }~
      pathPrefix("review"){
        pathEndOrSingleSlash{
          get{
            onSuccess(store ? ReadReviews){ case seq:Future[Seq[Review]]=>
              onComplete(seq) { // TODO: check for possible uses of streams to optimize return here
                case Failure(exception) => throw exception
                case Success(value) => complete(value)
              }
            }
          } ~
            post{
              entity(as[Review]){p=>
                onSuccess(store ? CreateReview(p)) {
                  case Failure(exception) => throw exception
                  case value:Review => complete(StatusCodes.Created,value)
                }
              }
            }
        } ~
          (path(Segment) | parameter("id".as[String])){ id:String =>
            get{
              onSuccess(store ? ReadReview(id)) {
                case Failure(exception) => throw exception
                case value:Review => complete(value)
              }
            } ~
              (put | patch){
                entity(as[Review]){ p=>
                  onSuccess(store ? UpdateReview(id,p)) {
                    case Failure(exception) => throw exception
                    case value:Review => complete(value)
                  }
                }
              } ~
              delete{
                onSuccess(store ? RemoveReview(id)) {
                  case Failure(exception) => throw exception
                  case value:Review => complete(StatusCodes.NoContent,value)
                }
              }
          }
      }
    }
  }
}
