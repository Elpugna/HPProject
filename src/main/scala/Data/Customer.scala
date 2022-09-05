package com.applaudostudios.fcastro.HPProject
package Data

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class Customer(id:Long, name:String=""){
}

trait CustomerProtocol extends DefaultJsonProtocol{
  implicit def formatter: RootJsonFormat[Customer] = jsonFormat2(Customer.apply)
}
