package com.applaudostudios.fcastro.HPProject
package Data

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class Product(id:String, title:String = "",category:String="")

case class RatingEntry(verified:Int, unverified:Int)
object Rating{

  def apply(reviews: Seq[Review]): Rating = {
    val aux: Array[RatingEntry] = Array.fill(5) {
      RatingEntry(0, 0)
    }
    var total: Double = 0.0d
    if (reviews.isEmpty) Rating(0, aux.toList)
    else {
      reviews.foreach { review =>
        val score: Int = review.rating - 1
        val prev = aux(score)
        total += review.rating
        aux(score) = if (review.verified.getOrElse(false)) prev.copy(verified = prev.verified + 1) else prev.copy(unverified = prev.unverified + 1)
      }
      Rating(total / reviews.size, aux)
    }
  }
}
case class Rating(total:Double,breakdown:Seq[RatingEntry])

trait ProductProtocol extends DefaultJsonProtocol{
  implicit def productFormatter: RootJsonFormat[Product] = jsonFormat3(Product.apply)
  implicit def ratingFormatter: RootJsonFormat[Rating] = jsonFormat2(Rating.apply)
  implicit def entryFormatter: RootJsonFormat[RatingEntry] = jsonFormat2(RatingEntry)
}

