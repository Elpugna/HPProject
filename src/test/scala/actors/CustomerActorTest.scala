package com.applaudostudios.fcastro.hp_project
package actors

import actors.CustomerActor.{Create, ReadReviewProducts, Update}
import actors.StoreActor.{ReadReviewIdList, ReadReviewedProducts}
import data.{Customer, Review}

import akka.actor._
import akka.testkit.TestProbe
import com.applaudostudios.fcastro.hp_project.UnitSpec
import org.scalatest.Outcome

import java.util.UUID
import scala.util.{Failure, Random}


class CustomerActorTest extends UnitSpec {

  case class FixtureParam(actor: ActorRef, probe:TestProbe, id:Long)
  override def withFixture(test: OneArgTest): Outcome = {
    // Perform setup
    val randomId: Long = Math.abs(Random.nextLong())
    val probe: TestProbe = TestProbe()
    val customerActor: ActorRef = system.actorOf(CustomerActor.props(randomId, probe.ref))
    val theFixture = FixtureParam(customerActor, probe, randomId)

    super.withFixture(test.toNoArgTest(theFixture)) // Invoke the test function
  }

  "A Customer Actor" should "Return the initial customer State" in { f =>
    f.actor ! Read
    expectMsg(Customer(f.id))
  }

  it should "Create Initialize the Customer info and preserve it after restart" in { f =>
    val prod = Customer(f.id, "Megamind")
    //When: Persisting the new product state
    f.actor ! Create(prod)
    expectMsg(prod)
    //and then restarting the actor
    f.actor ! PoisonPill
    val customerActor2 = system.actorOf(CustomerActor.props(f.id, f.probe.ref))
    //I should get the same product
    customerActor2 ! Read
    expectMsg(prod)
  }

  it should "Update the Customer info and preserve it after restart" in { f=>
    //When: Updating the customer state
    val customerUpdate = Customer(f.id,"Mark")
    f.actor ! Update(customerUpdate)
    expectMsg(customerUpdate)
    //and then restarting the actor
    f.actor ! PoisonPill
    val customerActor2 = system.actorOf(CustomerActor.props(f.id, f.probe.ref))
    //I should get the same product
    customerActor2 ! Read
    expectMsg(customerUpdate)
  }

  it should "Add add a review to the Customer and preserve it after restart" in { f =>
    val review = Review(id= "AWQE",helpful = 0,votes = 0)

    f.actor ! ReadReviewProducts
    f.probe.expectMsg(ReadReviewedProducts(Set().toList))

    f.actor ! AddReview("AWQE")
    f.probe.expectNoMessage()

    f.actor ! Kill
    val customerActor2 = system.actorOf(CustomerActor.props(f.id, f.probe.ref))

    customerActor2 ! ReadReviewIds
    f.probe.expectMsg(ReadReviewIdList(Set(review.id).toList))
  }

  //  it should "Remove an associated review by Id" in { f =>
  //    f.actor ! AddReview("ASD")
  //
  //    f.actor ! ReadReviewProducts
  //    f.probe.expectMsg(ReadReviewedProducts(Set("ASD").toList))
  //
  //    f.actor ! RemoveReview("ASD")
  //
  //    f.actor ! ReadReviewProducts
  //    f.probe.expectMsg(ReadReviewedProducts(Set("ASD").toList))
  //
  //    expectMsg(true)
  //  }
  //
  //  it should "Return a Failure containing a NotFoundException if the given id doesn't match with a valid Review" in { f =>
  //    val reviewId = UUID.randomUUID().toString
  //    f.actor ! RemoveReview(reviewId)
  //    expectMsg(Failure(NotFoundException(s"Customer ${f.id} did not have Review ${reviewId}")))
  //  }
}
