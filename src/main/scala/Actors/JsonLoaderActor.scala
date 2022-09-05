package com.applaudostudios.fcastro.HPProject
package Actors

import Actors.JsonLoaderActor.{JsonContents, LoadFile}
import Actors.StoreActor._
import Data._

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.DateTime
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl._
import akka.util.ByteString
import spray.json._

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}
import scala.language.postfixOps
import scala.util.Failure

object JsonLoaderActor{
  def props(store:ActorRef)=Props(new JsonLoaderActor(store))

  case class LoadFile(file: String)
  case class JsonContents(
  marketplace: String,
  customer_id: Long,
  review_id: String,
  product_id: String,
  product_parent: Long,
  product_title: String,
  product_category: String,
  star_rating: Int,
  helpful_votes: Long,
  total_votes: Long,
  vine: String,
  verified_purchase: String,
  review_headline: String,
  review_body: String,
  review_date: String
  )
}
trait JsonEntryProtocol extends DefaultJsonProtocol{

  implicit object MyStringJsonFormat extends JsonFormat[String] {
    def write(x: String) = JsString(x)
    def read(value: JsValue) = value match {
      case JsNumber(x) if x.isValidLong => x.toString()
      case JsString(x) => x
      case x => deserializationError("Expected String as JsString, but got " + x)
    }
  }
  implicit def formatter: RootJsonFormat[JsonContents] = jsonFormat15(JsonContents.apply)
}

class JsonLoaderActor(store:ActorRef) extends Actor with ActorLogging with SprayJsonSupport with JsonEntryProtocol with ReviewProtocol {
  def toData(jc:JsonContents): (Customer, Product, Review) = (
    Customer(jc.customer_id),
    Product(jc.product_id,jc.product_title,jc.product_category),
    Review(jc.review_id,
      jc.marketplace,
      jc.review_headline,
      jc.customer_id,
      jc.product_id,
      jc.review_body,
      jc.star_rating,
      jc.helpful_votes,
      jc.total_votes,
      DateTime.fromIsoDateTimeString(jc.review_date+"T00:00:00").get,
      Some(jc.vine=="Y"),
      Some(jc.verified_purchase=="Y"))
  )

  implicit val system: ActorSystem = context.system

  override def receive: Receive = {
    case LoadFile(file) if Files.exists(Paths.get(file))  =>
      val path = Paths.get(file);
      sender() ! s"OK, loading $path"
      var i =0
      val source = FileIO.fromPath(path)
      val selectJson: Flow[ByteString, ByteString, NotUsed] = JsonReader.select("$[*]")
      val parseJson: Flow[ByteString, (Customer, Product, Review), NotUsed] = Flow.fromFunction{ str:ByteString=>
        i=i+1
        log.info(s"Parsing #$i: ${str.utf8String}")
        val v = str.utf8String.parseJson.convertTo[JsonContents]
        log.info(s"Got $v")
        toData(v)
      }
      source
      .via(selectJson)
      .via(parseJson)
      .mapConcat{case (c:Customer,p:Product,r:Review)=>List(CreateCustomer(c),CreateProduct(p),CreateReview(r))}
      .via(Flow.fromFunction{a=>log.info(s"#$i: Generated Message ${a.toString}");a})
      .to(Sink.actorRefWithBackpressure(store,MassLoadStarted,MassLoadOver,t=>MassLoadFailure(t)))
      .run()
      /*.to(sink=Sink.foreach{
        case (c:Customer,p:Product,r:Review) =>
          log.info(s"Loading #$i: ${(c,p,r)}")
          store ! CreateCustomer(c)
          store ! CreateProduct(p)
          val rev = Await.result(store ? CreateReview(r),250 millis) // wait for last message to reduce throughput, ease pressure
          log.info(s"Review #$i: Got${(rev.toString)}")
          i+=1
      }
    ).run()*/
    case LoadFile(path) => sender() ! Failure(new FileNotFoundException())
    case a:Any => sender() ! s"Got un-matching: $a"
  }
}
