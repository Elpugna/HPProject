package com.applaudostudios.fcastro.hp_project

import actors.JsonLoaderActor.LoadFile
import actors.StoreActor._
import actors._
import data._

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.{
  EntityStreamingSupport,
  JsonEntityStreamingSupport
}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.pattern.ask
import akka.stream.scaladsl.Source

import java.io.FileNotFoundException
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class Router(store: ActorRef, loader: ActorRef)
    extends SprayJsonSupport
    with ProductProtocol
    with CustomerProtocol
    with ReviewProtocol
    with DAOProtocol {

  implicit val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case _: FileNotFoundException        => complete(404, "File not found.")
    case NotFoundException(message)      => complete(404, message)
    case x: AlreadyExistsException => complete(400, x.getMessage)
  }
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json()

  def routes: Route = {
    pathEndOrSingleSlash {
      getFromResource("web/index.html")
    } ~
      path(Remaining) { resource =>
        getFromResource("web/" + resource)
      } ~
      pathPrefix("api") {
        handleExceptions(exceptionHandler) {
          (pathPrefix("load") & post) {
            entity(as[(String, Long)]) { case (path, start) =>
              onSuccess(loader ? LoadFile(path, start)) {
                case Failure(exception) => throw exception
                case any: Any           => complete(any.toString)
              }
            }
          } ~
            (pathPrefix("stats") & pathEndOrSingleSlash & get) {
              onSuccess(store ? GetStats) {
                case Failure(exception) => throw exception
                case any: Any           => complete(any.toString)
              }
            } ~
            pathPrefix("product") {
              pathEndOrSingleSlash {
                get {
                  parameter("page".as[Int], "length".as[Int].withDefault(50)) {
                    (page, length) =>
                      onSuccess(store ? ReadProductsPaged(page, length)) {
                        case (seq: Future[Seq[Product]], len: Int) =>
                          onComplete(seq) {
                            case Failure(exception) => throw exception
                            case Success(value) =>
                              complete(
                                StatusCodes.OK,
                                DAO[Product](len, value.size, value)
                              )
                          }
                      }
                  } ~
                    onSuccess(store ? ReadProducts) {
                      case source: Source[Product, NotUsed] => complete(source)
                    }
                } ~
                  post {
                    entity(as[Product]) { p =>
                      onSuccess(store ? CreateProduct(p)) {
                        case Failure(exception) => throw exception
                        case value: Product =>
                          complete(StatusCodes.Created, value)
                      }
                    }
                  }
              } ~
                (pathPrefix(Segment) | parameter("id".as[String])) {
                  id: String =>
                    pathPrefix("reviews") {
                      pathPrefix("id") {
                        get {
                          onSuccess(store ? ReadProductReviewIds(id)) {
                            case Failure(exception) => throw exception
                            case seq: Seq[String]   => complete(seq)
                          }
                        }
                      } ~
                        pathPrefix("all") {
                          get {
                            onSuccess(store ? ReadProductReviews(id)) {
                              case s: Source[Review, NotUsed] => complete(s)
                              case Failure(exception)         => throw exception
                            }
                          }
                        }
                    } ~
                      pathPrefix("score") {
                        get {
                          onSuccess(store ? ReadProductScore(id)) {
                            case s: Source[ClientRating, NotUsed] => complete(s)
                            case Failure(exception)               => throw exception
                          }
                        }
                      } ~
                      get {
                        onSuccess(store ? ReadProduct(id)) {
                          case Failure(exception) => throw exception
                          case value: Product     => complete(value)
                        }
                      } ~
                      (put | patch) {
                        entity(as[Product]) { p =>
                          onSuccess(store ? UpdateProduct(id, p)) {
                            case Failure(exception) => throw exception
                            case value: Product     => complete(value)
                          }
                        }
                      } ~
                      delete {
                        onSuccess(store ? RemoveProduct(id)) {
                          case Failure(exception) => throw exception
                          case value: Product =>
                            complete(StatusCodes.NoContent, value)
                        }
                      }
                }
            } ~
            pathPrefix("customer") {
              pathEndOrSingleSlash {
                get {
                  parameter("page".as[Int], "length".as[Int].withDefault(50)) {
                    (page, length) =>
                      onSuccess(store ? ReadCustomersPaged(page, length)) {
                        case (seq: Future[Seq[Customer]], len: Int) =>
                          onComplete(seq) {
                            case Failure(exception) => throw exception
                            case Success(value) =>
                              complete(
                                StatusCodes.OK,
                                DAO[Customer](len, value.size, value)
                              )
                          }
                      }
                  } ~
                    onSuccess(store ? ReadCustomers) {
                      case source: Source[Customer, NotUsed] => complete(source)
                    }
                } ~
                  post {
                    entity(as[Customer]) { customer =>
                      onSuccess(store ? CreateCustomer(customer)) {
                        case Failure(exception) => throw exception
                        case value: Customer =>
                          complete(StatusCodes.Created, value)
                      }
                    }
                  }
              } ~
                (pathPrefix(LongNumber) | parameter("id".as[Long])) {
                  id: Long =>
                    pathPrefix("reviews") {
                      (pathPrefix("all") & get) {
                        onSuccess(store ? ReadCustomerReviewedProducts(id)) {
                          case s: Source[(Review, Product), NotUsed] =>
                            complete(s)
                          case Failure(exception) => throw exception
                        }
                      } ~
                        pathPrefix("id") {
                          get {
                            onSuccess(store ? ReadCustomerReviewIds(id)) {
                              case Failure(exception) => throw exception
                              case seq: Seq[String]   => complete(seq)
                            }
                          }
                        }
                    } ~
                      pathEndOrSingleSlash {
                        get {
                          onSuccess(store ? ReadCustomer(id)) {
                            case Failure(exception) => throw exception
                            case value: Customer    => complete(value)
                          }
                        } ~
                          (put | patch) {
                            entity(as[Customer]) { p =>
                              onSuccess(store ? UpdateCustomer(id, p)) {
                                case Failure(exception) => throw exception
                                case value: Customer    => complete(value)
                              }
                            }
                          } ~
                          delete {
                            onSuccess(store ? RemoveCustomer(id)) {
                              case Failure(exception) => throw exception
                              case value: Customer =>
                                complete(StatusCodes.NoContent, value)
                            }
                          }
                      }
                }
            } ~
            pathPrefix("review") {
              pathEndOrSingleSlash {
                get {
                  parameter("page".as[Int], "length".as[Int].withDefault(50)) {
                    (page, length) =>
                      onSuccess(store ? ReadReviewsPaged(page, length)) {
                        case (seq: Future[Seq[Review]], len: Int) =>
                          onComplete(seq) {
                            case Failure(exception) => throw exception
                            case Success(value) =>
                              complete(
                                StatusCodes.OK,
                                DAO[Review](len, value.size, value)
                              )
                          }
                      }
                  } ~
                    onSuccess(store ? ReadReviews) {
                      case source: Source[Review, NotUsed] => complete(source)
                    }
                } ~
                  post {
                    entity(as[Review]) { p =>
                      onSuccess(store ? CreateReview(p)) {
                        case Failure(exception) => throw exception
                        case value: Review =>
                          complete(StatusCodes.Created, value)
                      }
                    }
                  }
              } ~
                (pathPrefix(Segment) | parameter("id".as[String])) {
                  id: String =>
                    (pathPrefix("updateCustomer") & (put | patch) & entity(
                      as[String]
                    )) { customerId: String =>
                      onSuccess(
                        store ? UpdateReviewCustomer(id, customerId.toLong)
                      ) {
                        case Failure(exception) => throw exception
                        case value: Review      => complete(StatusCodes.OK, value)
                      }
                    } ~
                      (pathPrefix("updateProduct") & (put | patch) & entity(
                        as[String]
                      )) { productId: String =>
                        onSuccess(store ? UpdateReviewProduct(id, productId)) {
                          case Failure(exception) => throw exception
                          case value: Review      => complete(StatusCodes.OK, value)
                        }
                      } ~
                      pathEndOrSingleSlash {
                        get {
                          onSuccess(store ? ReadReview(id)) {
                            case Failure(exception) => throw exception
                            case value: Review      => complete(value)
                          }
                        } ~
                          (put | patch) {
                            entity(as[Review]) { p =>
                              onSuccess(store ? UpdateReview(id, p)) {
                                case Failure(exception) => throw exception
                                case value: Review      => complete(value)
                              }
                            }
                          } ~
                          delete {
                            onSuccess(store ? RemoveReview(id)) {
                              case Failure(exception) => throw exception
                              case value: Review =>
                                complete(StatusCodes.NoContent, value)
                            }
                          }
                      }
                }
            }
        }
      }
  }
}
