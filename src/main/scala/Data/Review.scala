package com.applaudostudios.fcastro.HPProject
package Data

import akka.http.scaladsl.model.DateTime
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}


case class Review(id:String,
                  region:String="",
                  title:String="",
                  customer:Long=0,
                  product:String="",
                  body:String="",
                  rating:Int=0,
                  helpful:Long,
                  votes:Long,
                  date:DateTime=DateTime.apply(0),
                  vine:Option[Boolean]=None,
                  verified:Option[Boolean]=None){
  require((0 to 5).contains(rating),"Star Rating must be from 0 to 5")
  require(helpful>=0)
  require(votes>=0)
  require(customer>=0)
}


trait ReviewProtocol extends DefaultJsonProtocol{
  implicit object DateFormat extends JsonFormat[DateTime] {
    def write(date: DateTime): JsString = JsString(date.toIsoDateString())
    def read(json: JsValue): DateTime = json match {
      case JsString(rawDate) =>
        DateTime.fromIsoDateTimeString(rawDate+"T00:00:00")
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }
  }
  implicit def reviewFormatter: RootJsonFormat[Review] = jsonFormat12(Review.apply)
}
