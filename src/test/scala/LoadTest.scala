package com.applaudostudios.fcastro.HPProject

import Actors.JsonEntryProtocol
import Actors.JsonLoaderActor.JsonContents

import akka.actor.ActorSystem
import akka.event.slf4j.Logger
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.DateTime
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{FileIO, Flow, Sink}
import com.applaudostudios.fcastro.HPProject.Data.{Customer, Product, Review, ReviewProtocol}
import spray.json._

import java.nio.file.Paths

object LoadTest extends App with SprayJsonSupport with JsonEntryProtocol with ReviewProtocol{
  implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
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
  println("{\"customer_id\":24371595,\"helpful_votes\":0,\"marketplace\":\"US\",\"product_category\":\"Gift Card\",\"product_id\":\"B004LLIL5A\",\"product_parent\":346014806,\"product_title\":\"Amazon eGift Card - Celebrate\",\"review_body\":\"Great birthday gift for a young adult.\",\"review_date\":\"2015-08-31\",\"review_headline\":\"Five Stars\",\"review_id\":\"R27ZP1F1CD0C3Y\",\"star_rating\":5,\"total_votes\":0,\"verified_purchase\":\"Y\",\"vine\":\"N\"}"
    .parseJson.toString())
  println(toData("{\"customer_id\":24371595,\"helpful_votes\":0,\"marketplace\":\"US\",\"product_category\":\"Gift Card\",\"product_id\":\"B004LLIL5A\",\"product_parent\":346014806,\"product_title\":\"Amazon eGift Card - Celebrate\",\"review_body\":\"Great birthday gift for a young adult.\",\"review_date\":\"2015-08-31\",\"review_headline\":\"Five Stars\",\"review_id\":\"R27ZP1F1CD0C3Y\",\"star_rating\":5,\"total_votes\":0,\"verified_purchase\":\"Y\",\"vine\":\"N\"}"
    .parseJson.convertTo[JsonContents]).toString())

  FileIO.fromPath(Paths.get("C:\\Users\\Administrator\\Documents\\Applaudo\\Lightbend\\HPProyect\\giftcards.json"))
    .via(JsonReader.select("$[*]"))
    .via(Flow.fromFunction{in=>
      Logger.root.info(in.utf8String)
      in
    })
    .via(Flow.fromFunction{str=>
      Logger.root.info(str.utf8String)
      Logger.root.info(str.utf8String.parseJson.toString())
      Logger.root.info(str.utf8String.parseJson.convertTo[JsonContents].toString)
      val t = toData(str.utf8String.parseJson.convertTo[JsonContents])
      Logger.root.info(t.toString())
    })
    .to(Sink.foreach{jC=>Logger.root.info(jC.toString)})
    .run()
}
