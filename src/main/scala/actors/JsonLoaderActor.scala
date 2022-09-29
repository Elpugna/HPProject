package com.applaudostudios.fcastro.hp_project
package actors

import actors.JsonLoaderActor._
import actors.StoreActor._
import data._

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
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Failure

object JsonLoaderActor {
  def props(store: ActorRef): Props = Props(new JsonLoaderActor(store))

  trait JsonEntryProtocol extends DefaultJsonProtocol {

    implicit object MyStringJsonFormat extends JsonFormat[String] {
      def write(x: String): JsString = JsString(x)

      def read(value: JsValue): String = value match {
        case JsNumber(x) if x.isValidLong => x.toString()
        case JsString(x)                  => x
        case x =>
          deserializationError("Expected String as JsString, but got " + x)
      }
    }

    implicit def formatter: RootJsonFormat[JsonContents] = jsonFormat15(
      JsonContents.apply
    )
  }

  case class LoadFile(file: String, start: Long = 0)

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

class JsonLoaderActor(store: ActorRef)
    extends Actor
    with ActorLogging
    with SprayJsonSupport
    with JsonEntryProtocol
    with ReviewProtocol {

  override def receive: Receive = {
    case LoadFile(file, start: Long) if Files.exists(Paths.get(file)) =>
      val path = Paths.get(file)
      sender() ! s"OK, loading $path starting from $start"
      val source = FileIO.fromPath(path)
      val selectJson: Flow[ByteString, ByteString, NotUsed] =
        JsonReader.select("$[*]")
      val parseJson: Flow[ByteString, (Customer, Product, Review), NotUsed] =
        Flow.fromFunction { str: ByteString =>
          val v = str.utf8String.parseJson.convertTo[JsonContents]
          toData(v)
        }
      source
        .via(selectJson.drop(start).throttle(500, 12 seconds))
        .via(parseJson)
        .mapConcat { case (c: Customer, p: Product, r: Review) =>
          List(CreateCustomer(c), CreateProduct(p), CreateReview(r))
        }
        .to(
          Sink.actorRefWithBackpressure(
            store,
            MassLoadStarted,
            MassLoadOver,
            t => MassLoadFailure(t)
          )
        )
        .run()
    case LoadFile(_, _) => sender() ! Failure(new FileNotFoundException())
    case a: Any         => sender() ! s"Got un-matching: $a"
  }

  implicit val system: ActorSystem = context.system

  def toData(jc: JsonContents): (Customer, Product, Review) = (
    Customer(jc.customer_id),
    Product(jc.product_id, jc.product_title, jc.product_category),
    Review(
      jc.review_id,
      jc.marketplace,
      jc.review_headline,
      jc.customer_id,
      jc.product_id,
      jc.review_body,
      jc.star_rating,
      jc.helpful_votes,
      jc.total_votes,
      DateTime.fromIsoDateTimeString(jc.review_date + "T00:00:00").get,
      Some(jc.vine == "Y"),
      Some(jc.verified_purchase == "Y")
    )
  )
}
