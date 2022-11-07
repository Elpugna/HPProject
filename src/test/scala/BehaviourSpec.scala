package com.applaudostudios.fcastro.hp_project

import data.{CustomerProtocol, ProductProtocol, ReviewProtocol}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.{AnyFeatureSpec, FixtureAnyFeatureSpecLike}
import org.scalatest.matchers.should.Matchers

abstract class BehaviourSpec extends FixtureAnyFeatureSpecLike with GivenWhenThen with Matchers with SprayJsonSupport with ProductProtocol with CustomerProtocol with ReviewProtocol with ScalatestRouteTest{

}
