package com.applaudostudios.fcastro.hp_project

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import org.scalatest.flatspec.{AnyFlatSpec, AnyFlatSpecLike, FixtureAnyFlatSpecLike}
import org.scalatest.matchers.must.Matchers

abstract class UnitSpec
  extends TestKit(ActorSystem("TestActorSpec"))
    with FixtureAnyFlatSpecLike
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with BeforeAndAfterAll
    with ImplicitSender{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}

