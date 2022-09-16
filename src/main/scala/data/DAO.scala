package com.applaudostudios.fcastro.hp_project
package data

import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}

case class DAO[T](total: Int = 1, length: Int = 1, data: Seq[T]) {}

trait DAOProtocol extends DefaultJsonProtocol {
  implicit def myDAOFormatter[A: JsonFormat]: RootJsonFormat[DAO[A]] =
    jsonFormat3(DAO.apply[A])
}
