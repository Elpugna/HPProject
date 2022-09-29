package com.applaudostudios.fcastro.hp_project
package data

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class Product(id: String, title: String = "", category: String = "")

case class RatingEntry(verified: Int, unverified: Int) {
  def total: Int = verified + unverified
}

object Rating {
  def apply(reviews: Seq[Review]): Rating = {
    reviews.foldLeft(apply()) { (acc, r) =>
      acc.add(r.rating, r.verified.getOrElse(false))
    }
  }

  def apply(): Rating = {
    Rating(
      0,
      Array.fill(5) {
        RatingEntry(0, 0)
      }
    )
  }

}

case class Rating(cumulative: Long, breakdown: Seq[RatingEntry]) {
  def add(score: Int, verified: Boolean): Rating = {
    val prev = breakdown(score - 1)
    val n =
      if (verified) prev.copy(verified = prev.verified + 1)
      else prev.copy(unverified = prev.unverified + 1)
    Rating(cumulative + score, breakdown.updated(score - 1, n))
  }

  def visual: ClientRating = ClientRating(total, breakdown)

  def total: Double = cumulative / breakdown.foldLeft(0) { (acc, entry) =>
    entry.total + acc
  }
}

case class ClientRating(average: Double, breakdown: Seq[RatingEntry])

trait ProductProtocol extends DefaultJsonProtocol {
  implicit def productFormatter: RootJsonFormat[Product] = jsonFormat3(
    Product.apply
  )

  implicit def ratingFormatter: RootJsonFormat[ClientRating] = jsonFormat2(
    ClientRating.apply
  )

  implicit def entryFormatter: RootJsonFormat[RatingEntry] = jsonFormat2(
    RatingEntry
  )
}
