package com.applaudostudios.fcastro.HPProject
package Data

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class Product(id:String, title:String = "",category:String="")
case class Rating(total:Double,breakdown:Seq[(Int,Int)])
trait ProductProtocol extends DefaultJsonProtocol{
  implicit def productFormatter: RootJsonFormat[Product] = jsonFormat3(Product.apply)
  implicit def ratingFormatter: RootJsonFormat[Rating] = jsonFormat2(Rating.apply)
}

