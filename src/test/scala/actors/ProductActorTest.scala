package com.applaudostudios.fcastro.hp_project
package actors

import actors.ProductActor.{Create, ReadReviews, Update}
import actors.StoreActor.{ReadReviewIdList, ReadReviewList}
import data.{Product, Review}

import akka.actor.{ActorRef, Kill, PoisonPill}
import akka.testkit.TestProbe
import com.applaudostudios.fcastro.hp_project.UnitSpec
import org.scalatest.Outcome

import java.util.UUID
import scala.util.Failure



class ProductActorTest extends UnitSpec {

  case class FixtureParam(actor: ActorRef, probe:TestProbe, id:String)
  override def withFixture(test: OneArgTest): Outcome = {
    // Perform setup
    val randomId: String = UUID.randomUUID().toString
    val probe: TestProbe = TestProbe()
    val productActor: ActorRef = system.actorOf(ProductActor.props(randomId, probe.ref))
    val theFixture = FixtureParam(productActor, probe, randomId)

    super.withFixture(test.toNoArgTest(theFixture)) // Invoke the test function

  }

  "A Product Actor" should "Return the initial product State" in { f =>
    f.actor ! Read
    expectMsg(Product(f.id))
  }

  it should "Create Initialize the Product value and preserve it after restart" in { f =>
    val prod = Product(f.id, "watch", "A1XDA")
    //When: Persisting the new product state
    f.actor ! Create(prod)
    expectMsg(prod)
    //and then restarting the actor
    f.actor ! PoisonPill
    val productActor2 = system.actorOf(ProductActor.props(f.id, f.probe.ref))
    //I should get the same product
    productActor2 ! Read
    expectMsg(prod)
  }

  it should "Update the product info and preserve it after restart" in { f=>
    //When: Updating the product state
    val prodUpdate = Product(f.id,"Cellphone")
    f.actor ! Update(prodUpdate)
    expectMsg(prodUpdate)
    //and then restarting the actor
    f.actor ! PoisonPill
    val productActor2 = system.actorOf(ProductActor.props(f.id, f.probe.ref))
    //I should get the same product
    productActor2 ! Read
    expectMsg(prodUpdate)
  }

  it should "Add add a review to the product and preserve it after restart" in { f =>
    val review = Review(id= "AWQE",helpful = 0,votes = 0)

    f.actor ! ReadReviews
    f.probe.expectMsg(ReadReviewList(Set().toList))

    f.actor ! AddReview("AWQE")
    f.probe.expectNoMessage()

    f.actor ! Kill
    val productActor2 = system.actorOf(ProductActor.props(f.id, f.probe.ref))

    productActor2 ! ReadReviewIds
    f.probe.expectMsg(ReadReviewIdList(Set(review.id).toList))
  }

  it should "Remove an associated review by Id" in { f =>
    f.actor ! AddReview("AWQE")
    f.actor ! RemoveReview("AWQE")
    expectMsg(true)
  }
  it should "Return a Failure containing a NotFoundException if the given id doesn't match with a valid Review" in { f =>
    val reviewId = UUID.randomUUID().toString
    f.actor ! RemoveReview(reviewId)
    expectMsg(Failure(NotFoundException(s"Product ${f.id} did not have Review $reviewId")))
  }
}
