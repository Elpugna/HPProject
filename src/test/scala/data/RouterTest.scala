package com.applaudostudios.fcastro.hp_project
package data

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model._
import actors.StoreActor
import akka.http.scaladsl.server._
import actors.StoreActor.{CreateCustomer, CreateProduct, CreateReview}
import org.scalatest.Outcome
import spray.json._

class RouterTest extends BehaviourSpec{

  val store: ActorRef = system.actorOf(Props(new StoreActor()), "StoreActor")
  val router: Router = Router(store, Actor.noSender)
  val route: Route = router.routes

  case class FixtureParam(prod: Product, customer:Customer, review:Review, dumbId:String)
  override def withFixture(test: OneArgTest): Outcome = {
    val product = Product("TheBrandNewProdId", "Product Name Remix", "Product Category Remastered")
    val customer = Customer(111L, "Charles Darwin")
    val review = Review(
      "TheBrandNewReview",
      "America",
      "Five Stars",
      111L,
      "TheBrandNewProdId",
      "Excelent",
      5,
      0,
      0,
      vine = Some(false),
      verified = Some(true))
    val dumbId = "11223344"
    val theFixture = FixtureParam(product, customer, review, dumbId)
    super.withFixture(test.toNoArgTest(theFixture)) // Invoke the test function
  }


  Feature("Status Read") {
    Scenario("As an Administrator I want to be able to know the health and some useful stats of the application") { f =>
      Given("The app up and running")

//      When("Executing the GET request to api/stats")
      When("Executing the request retrieving statistics from the backend API at /api/stats")
      Get("/api/stats") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.isSuccess() shouldBe true
        responseAs[String] shouldEqual "Store has:\n0 Products\n0 Customers\n0 Reviews"
      }
    }
  }

  Feature("Product Creation") {
    Scenario("As a Client I want to be able to create a new Product") { f=>
      Given("A new product that I want to create")
      When("Sending the request that creates the product")
      Post("/api/product", f.prod) ~> route ~> check {
        Then("The response status should be 201 CREATED")
        status.intValue() shouldBe 201
        And("Return the created product")
        responseAs[Product] shouldEqual f.prod
      }
    }
    Scenario("As a Client I want to not be able to add an already registered Product") { f =>
      Given("A product already created")
      When("Sending the request that creates the already registered product")
      Post("/api/product", f.prod) ~> route ~> check {
        Then("The response status should be 400 BAD REQUEST")
        status.intValue() shouldBe 400
      }
    }
  }
  Feature("Customer Creation") {
    Scenario("As a Client I want to be able to create a new Customer") { f =>
      Given("A new customer that I want to create")
      When("Sending the request that creates the Customer")
      Post("/api/customer", f.customer) ~> route ~> check {
        Then("The response status should be 201 CREATED")
        status.intValue() shouldBe 201
        And("Return the created customer")
        responseAs[Customer] shouldEqual f.customer
      }
    }
    Scenario("As a Client I want to not be able to create an already registered Customer") { f =>
      Given("A customer already created")

      When("Sending the request that creates the already registered Customer")
      Post("/api/customer", f.customer) ~> route ~> check {
        Then("The response status should be 400 BAD REQUEST")
        status.intValue() shouldBe 400
      }
    }
  }
  Feature("Review Creation") {
    Scenario("As a Client I want to be able to create a new Review") { f=>
      Given("A new review that I want to add")

      When("Sending the request that creates the Review")
      Post("/api/review", f.review) ~> route ~> check {
        Then("The response status should be 201 CREATED")
        status.intValue() shouldBe 201
        And("Return the created review")
        responseAs[Review] shouldEqual f.review
      }
    }


    Scenario("As a Client I want to not be able to add an already registered Review") { f=>
      Given("A review already added")

      When("Sending the request that creates the already registered Review")
      Post("/api/review", f.review) ~> route ~> check {
        Then("The response status should be 400 BAD REQUEST")
        status.intValue() shouldBe 400
      }
    }
    Scenario("As a Client I want to not be able to add a Review if the Review's Customer_id does not match with an existent customer") { f=>
      Given("A new review with an invalid Customer_Id field that I want to add")
      val newReview =
        Review(
          "AnotherReviewId",
          "America",
          "Five Stars",
          999L, //The non registered customer Id
          "ASDASD",
          "Excelent",
          5,
          0,
          0,
          vine = Some(false),
          verified = Some(true))
      When("Sending the request that creates the Review")
      Post("/api/review", newReview) ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
    Scenario("As a Client I want to not be able to add a Review if the Review's Product_Id does not match with an existent Product") { f =>
      Given("A new review with an invalid Product_Id field that I want to add")
      val newReview =
        Review(
          "OtherPeculiarId",
          "America",
          "Five Stars",
          111L,
          "ThisIdIsFromAProductOfAnotherStore",
          "Excelent",
          5,
          0,
          0,
          vine = Some(false),
          verified = Some(true))
      When("Sending the request that creates the Review")
      Post("/api/review", newReview) ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }


  Feature("Product Deletion") {
    Scenario("As a Client I want to be able to Delete a Product") { f =>
      Given("An already stored product")
      And("The product's id as a parameter")
      val newProd = Product("Test", "SomeName", "SomeCategory")
      store ! CreateProduct(newProd)
      When("Sending the request that deletes the existing Product")
      Delete(s"/api/product?id=${newProd.id}") ~> route ~> check {
        Then("The response status should be 204 NO CONTENT")
        status.intValue() shouldBe 204
      }
    }

    Scenario("As a Client I want to not be able to Delete a non existent product") { f =>
      Given("Some product's Id that does not matches any actual product")

      When("Sending the request that deletes the product with the provided id")
      Delete(s"/api/product?id=${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }
  Feature("Customer Deletion") {
    Scenario("As a Client I want to be able to Delete a Customer") { f=>
      Given("An already stored customer")
      And("The customer's id as a parameter")
      val newCust = Customer(1001L, "SomeName")
      store ! CreateCustomer(newCust)
      When("Sending the request that deletes the existing Customer")
      Delete(s"/api/customer?id=${newCust.id}") ~> route ~> check {
        Then("The response status should be 204 NO CONTENT")
        status.intValue() shouldBe 204
      }
    }

    Scenario("As a Client I want to not be able to Delete a non existent customer") { f =>
      Given("Some Customer's Id that does not matches any actual Customer")

      When("Sending the request that deletes the customer with the provided id")
      Delete(s"/api/customer?id=${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }
  Feature("Review Deletion") {
    Scenario("As a Client I want to be able to Delete a Review") { f=>
      Given("An already stored Review")
      And("The review's id as a parameter")
      val newRev = Review(
        "AAAAAAA",
        "America",
        "Tree Stars",
        111L,
        "TheBrandNewProdId",
        "Excelent",
        5,
        0,
        0,
        vine = Some(false),
        verified = Some(true))
      store ! CreateReview(newRev)
      When("Sending the request that deletes the existing Review")
      Delete(s"/api/review?id=${newRev.id}") ~> route ~> check {
        Then("The response status should be 204 NO CONTENT")
        status.intValue() shouldBe 204
      }
    }

    Scenario("As a Client I want to not be able to Delete a non existent Review") { f=>
      Given("Some Review's Id that does not matches any actual Customer")


      When("Sending the request that deletes the customer with the provided id")
      Delete(s"/api/review?id=${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }


  Feature("Product Read") {
    Scenario("As a Client I want to be able to read all the Products") { f =>
      Given("Some products already registered in the app")

      When("Sending the request that retrieves all the products from the API")
      Get("/api/product") ~> route ~> check {
        Then("I should receive an Array containing the registered products")
        status shouldBe StatusCodes.OK
        responseAs[JsArray].elements.size shouldBe 1
        responseAs[JsArray].elements(0).convertTo[Product] shouldEqual f.prod
      }
    }

    Scenario("As a Client I want to be able to view the Products with Pagination") { f=>
      Given("Some products already registered in the app")

      And("Some 'page' Integer number request parameter")
      And("Some 'length' Integer number request parameter ")
      val page = 0
      val length = 15
      When(s"Sending the request that retrieves the {length} products from the API starting from the {page * length}th")
      Get(s"/api/product?page=$page&length=$length") ~> route ~> check {
        Then("I should receive a 200 OK status code")
        status shouldBe StatusCodes.OK
        And(s"I should receive a list containing at most $length of the registered products")
        val total = responseAs[JsObject].getFields("total")
        val data = responseAs[JsObject].getFields("data")
        total.head.toString() shouldBe "1"
        data.head.asInstanceOf[JsArray].elements(0).convertTo[Product] shouldBe f.prod
      }

    //Todo distinct - UNhappy Path

//      When(s"Executing the GET request to api/product?page=0&length=$length")
//      And("There are products to be shown in the respective page")
//      Get(s"/api/product?page=0&length=$length") ~> route ~> check {
//        Then("I should receive a 200 OK status code")
//        status shouldBe StatusCodes.OK
//        And(s"I should receive a list containing at most $length of the registered products")
//        val total = responseAs[JsObject].getFields("total")
//        val data = responseAs[JsObject].getFields("data")
//        total.head.toString() shouldBe "1"
//        data.head.asInstanceOf[JsArray].elements(0).convertTo[Product] shouldBe alreadyAddedProd
//      }
    }
  }
  Feature("Customer Read") {
    Scenario("As a Client I want to be able to view all the Customers") { f =>
      Given("Some customers already registered in the app")

      When("Sending the request that retrieves all the Customers from the API")
      Get("/api/customer") ~> route ~> check {
        Then("I should receive an Array containing the registered customers")
        status shouldBe StatusCodes.OK
        responseAs[JsArray].elements.size shouldBe 1
        responseAs[JsArray].elements(0).convertTo[Customer] shouldEqual f.customer
      }
    }

    Scenario("As a Client I want to be able to view the Customers with Pagination") { f=>
      Given("Some customers already registered in the app")
      And("'Page' Integer number request parameter")
      And("'Length' Integer number request parameter ")
      val page = 0
      val length = 15
      When(s"Sending the request that retrieves the {length} customers from the API starting from the {page * length}th")
      Get(s"/api/customer?page=$page&length=$length") ~> route ~> check {
        Then("I should receive a 200 OK status code")
        status shouldBe StatusCodes.OK
        And(s"I should receive a list containing at most $length of the registered Customers")
        val total = responseAs[JsObject].getFields("total")
        val data = responseAs[JsObject].getFields("data")
        total.head.toString() shouldBe "1"
        data.head.asInstanceOf[JsArray].elements(0).convertTo[Customer] shouldBe f.customer
      }
      //Todo Unhappy path - StatusCode Refactor 449
//      When(s"Executing the GET request to api/customer?page=0&length=$length")
//      And("There are Customers to be shown in the respective page")
//      Get(s"/api/customer?page=0&length=$length") ~> route ~> check {
//        Then("I should receive a 200 OK status code")
//        status shouldBe StatusCodes.OK
//        And(s"I should receive a list containing at most $length of the registered Customers")
//        val total = responseAs[JsObject].getFields("total")
//        val data = responseAs[JsObject].getFields("data")
//        total.head.toString() shouldBe "1"
//        data.head.asInstanceOf[JsArray].elements(0).convertTo[Customer] shouldBe alreadyAddedCustomer
//      }
    }
  }
  Feature("Review Read") {
    Scenario("As a Client I want to be able to view all the Reviews") { f=>
      Given("Some reviews already registered in the app")

      When("Sending the request that retrieves all the Reviews from the API")
      Get("/api/review") ~> route ~> check {
        Then("I should receive an Array containing the registered Reviews")
        status shouldBe StatusCodes.OK
        responseAs[JsArray].elements.size shouldBe 1
        responseAs[JsArray].elements(0).convertTo[Review] shouldEqual f.review
      }
    }

    Scenario("As a Client I want to view the Reviews with Pagination") { f =>
      Given("Some reviews already registered in the app")
      And("'Page' Integer number request parameter")
      And("'Length' Integer number request parameter ")
      val page = 0
      val length = 15
      When(s"Sending the request that retrieves the {length} Reviews from the API starting from the {page * length}th")
      Get(s"/api/review?page=$page&length=$length") ~> route ~> check {
        Then("I should receive a 200 OK status code")
        status shouldBe StatusCodes.OK
        And(s"I should receive a list containing at most $length of the registered Reviews")
        val total = responseAs[JsObject].getFields("total")
        val data = responseAs[JsObject].getFields("data")
        total.head.toString() shouldBe "1"
        data.head.asInstanceOf[JsArray].elements(0).convertTo[Review] shouldBe f.review
      }

      //Todo Unhappy path - StatusCode Refactor 449
//      When(s"Executing the GET request to api/review?page=0&length=$length")
//      And("There are Reviews to be shown in the respective page")
//      Get(s"/api/review?page=0&length=$length") ~> route ~> check {
//        Then("I should receive a 200 OK status code")
//        status shouldBe StatusCodes.OK
//        And(s"I should receive a list containing at most $length of the registered Reviews")
//        val total = responseAs[JsObject].getFields("total")
//        val data = responseAs[JsObject].getFields("data")
//        total.head.toString() shouldBe "1"
//        data.head.asInstanceOf[JsArray].elements(0).convertTo[Review] shouldBe alreadyAddedReview
//      }
    }
  }


  Feature("Reading a Product by Id") {
    Scenario("As a Client I want to be able to get a Product's info with the Id") { f=>
      Given("The Id of an already existent product")

      When("Sending the request that Retrieves the product info from the API")
      Get(s"/api/product/${f.prod.id}") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the Product's info")
        responseAs[Product] shouldEqual f.prod
      }
    }

    Scenario("As a Client I want to not be able to get a Product's info with an invalid Id") { f=>
      Given("The Id of a non existent product")
      When("Sending the request that Retrieves the product info from the API")
      Get(s"/api/product/${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }
  Feature("Reading a Review by Id") {
    Scenario("As a Client I want to be able to get a Review's info with the Id") { f=>
      Given("The Id of an already existent Review")

      When("Sending the request that retrieves the Review info from the API")
      Get(s"/api/review/${f.review.id}") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the review's info")
        responseAs[Review] shouldEqual f.review
      }
    }

    Scenario("As a Client I want to not be able to get a Review's info with an invalid Id"){f=>
      Given("A non existing Review Id")

      When("Sending the request that retrieves the Review info from the API")
      Get(s"/api/review/${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }
  Feature("Reading a Customer by Id") {
    Scenario("As a Client I want to be able to get a Customer's info with the Id") { f=>
      Given("The Id of an already existent Customer")

      When("Sending the request that retrieves the Customer info from the API")
      Get(s"/api/customer/${f.customer.id}") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Return the Customer's info")
        responseAs[Customer] shouldEqual f.customer
      }
    }

    Scenario("As a Client I want to not be able to get a Review's info with an invalid Id") { f=>
      Given("A non existing Customer Id")

      When("Sending the request that retrieves the Customer info from the API")
      Get(s"/api/customer/${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }

  Feature("Reading Product's Reviews") {
    Scenario("As a Client I want to be able to view ALL the reviews associated to a product") { f=>
      Given("An existing Product with some associated Reviews")
      val anotherReview = Review(
        "TheSecondReview",
        "Europe",
        "Five Stars",
        111L,
        "TheBrandNewProdId",
        "Excelent",
        5,
        0,
        0,
        vine = Some(false),
        verified = Some(true))
      store ! CreateReview(anotherReview)

      When("Sending the request that retrieves all the Reviews for that specific product")
      Get(s"/api/product/${f.prod.id}/reviews/all") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return a List with the associated Reviews")
        responseAs[JsArray].elements.size shouldEqual 2
        responseAs[JsArray].elements(0).convertTo[Review] shouldEqual anotherReview
      }
    }
    Scenario("As a Client I want to get a List of all the Review_Ids associated with a Product") { f=>
      Given("An existing Product with some associated Reviews")
      val anotherReview = Review(
        "TheSecondReview",
        "Europe",
        "Five Stars",
        111L,
        "TheBrandNewProdId",
        "Excelent",
        5,
        0,
        0,
        vine = Some(false),
        verified = Some(true))

      When("Sending the request that retrieves a List of the Review_id's for that specific product")
      Get(s"/api/product/${f.prod.id}/reviews/id") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return a List containing the Ids of the reviews associated with that product")
        responseAs[JsArray].elements(1).convertTo[String] shouldEqual anotherReview.id
      }
    }

    Scenario("As a Client I want to not be able to view all the reviews when the Product Id is invalid") { f=>
      Given("A non existing Product Id")

      When(s"Sending the request to retrieve all the reviews for that product")
      Get(s"/api/product/${f.dumbId}/reviews/all") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
        And("It should contain a valid message")
        responseAs[String] shouldBe "The requested product was not available or did not exist"
      }
    }
    Scenario("As a Client I want to not be able to view a List of all the Review_Ids when the Product Id is invalid"){ f=>
      Given("A non existing Product Id")

      When("Sending the request that retrieves a List of the Review_id's for that specific product")
      Get(s"/api/product/${f.dumbId}/reviews/id") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }

  Feature("Reading Product Score") {
    Scenario("As a Client I want to be able to get the score of a given Product") {f=>
      Given("An existing Product with associated Reviews (and scores)")

      When("Sending the request that retrieves the score of the product")
      Get(s"/api/product/${f.prod.id}/score") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("The response must contain the product's average score")
        val average = responseAs[JsArray].elements(0).convertTo[JsObject].getFields("average")
        average.head.convertTo[Double] shouldBe 5D
      }
    }
    Scenario("As a Client I want to not be able to get the score of an invalid Product") { f=>
      Given("A non existent Product Id")

      When("Sending the request that retrieves the score of the product")
      Get(s"/api/product/${f.dumbId}/score") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
      }
    }
  }

  Feature("Reading Customer's Reviews by Product") {
    Scenario("As a Client I want to be able to view ALL the reviews associated to each product for a specific Customer") { f=>
      Given("An existing Customer with some Reviewed Products")

      When("Sending the request that retrieves all the Reviews by product made by a specific Customer")
      Get(s"/api/customer/reviews/all?id=${f.customer.id}") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return a List containing a List of Reviews and product")
        responseAs[JsArray].elements.size shouldEqual 2
        responseAs[JsArray].elements(0).convertTo[JsArray].elements(0).convertTo[Review].customer shouldBe f.customer.id
      }
    }
    Scenario("As a Client I want to get a List of all the Review_Ids associated with a specific Customer") { f=>
      Given("An existing Customer with some associated Reviews")
      When("Sending the request that retrieves a List of the Review_id's for that specific product")
      Get(s"/api/customer/reviews/id?id=${f.customer.id}") ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return a List containing the Ids of the reviews associated with that product")
        responseAs[JsArray].elements(0).convertTo[String] shouldEqual f.review.id
      }
    }

    Scenario("As a Client I want to not be able to view all the reviews when the Product Id is invalid") { f=>
      Given("An existing Customer with some Reviewed Products")

      When("Sending the request that retrieves all the Reviews by product made by a specific Customer")
      Get(s"/api/customer/reviews/all?id=${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
        And("It should contain a valid message")
        responseAs[String] shouldBe "The requested customer was not available or did not exist"
      }
    }
    Scenario("As a Client I want to not be able to view a List of all the Review_Ids when the Product Id is invalid") { f=>
      Given("An existing Customer with some Reviewed Products")

      When("Sending the request that retrieves all the Reviews by product made by a specific Customer")
      Get(s"/api/customer/reviews/all?id=${f.dumbId}") ~> route ~> check {
        Then("The response status should be 404 NOT FOUND")
        status.intValue() shouldBe 404
        And("It should contain a valid message")
        responseAs[String] shouldBe "The requested customer was not available or did not exist"
      }
    }
  }

  Feature("Updating a Product") {
    Scenario("As a Client I want to be able to update some Product's Info") { f=>
      Given("An already existing product")
      And("A new product with the updated information")
      val theNewProd = Product("TheBrandNewProdId", "This name is Remastered", "The Category Remains The same")

      When("Sending the request that updates the existing Product to the API")
      Put(s"/api/product?id=${f.prod.id}", theNewProd) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Product")
        responseAs[Product] shouldBe theNewProd
      }
    }
    Scenario("As a Client I want to be able to update (by patching) some Product's Info") {f=>
      Given("An already existing product")
      And("A new product with the updated information")
      val theNewProd = Product("TheBrandNewProdId", "This name is Remastered", "The Category Remains The same")


      When("Sending the request that updates the existing Product to the API")
      Patch(s"/api/product?id=${theNewProd.id}", f.prod) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Product")
        responseAs[Product] shouldBe f.prod
      }
    }
  }
  Feature("Updating a Customer") {
    Scenario("As a Client I want to be able to update some Customer's Info") { f=>
      Given("An already existing customer")
      And("A new Customer with the updated information")
      val theNewCust = Customer(111L, "Kendrik Lamark")
      When("The info")
      When("Sending the request that updates the existing Customer to the API")
      Put(s"/api/customer?id=${f.customer.id}", theNewCust) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Customer")
        responseAs[Customer] shouldBe theNewCust
      }
    }
    Scenario("As a Client I want to be able to update (by patching) some Customer's Info") { f=>
      Given("An already existing customer")
      And("A new Customer with the updated information")
      val theNewCust = Customer(111L, "Kendrik Lamark")
      When("The info")
      When("Sending the request that updates the existing Customer to the API")
      Patch(s"/api/customer?id=${f.customer.id}", f.customer) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Customer")
        responseAs[Customer] shouldBe f.customer
      }
    }
  }
  Feature("Updating a Review") {
    Scenario("As a Client I want to be able to update some Review's Info") { f=>
      Given("An already existing review")
      And("A new review with the updated information")
      val theNewRev = Review(
        "TheBrandNewReview",
        "Europe",
        "Tree Stars",
        111L,
        "TheBrandNewProdId",
        "Good",
        3,
        0,
        0,
        vine = Some(false),
        verified = Some(true))
      When("The info")
      When("Sending the request that updates the existing Product to the API")
      Put(s"/api/review/${f.review.id}", theNewRev) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Product")
        responseAs[Review] shouldBe theNewRev
      }
    }
    Scenario("As a Client I want to be able to update (by patching) some Review's Info") { f=>
      Given("An already existing review")
      And("A new review with the updated information")
      val theNewRev = Review(
        "TheBrandNewReview",
        "Europe",
        "Tree Stars",
        111L,
        "TheBrandNewProdId",
        "Good",
        3,
        0,
        0,
        vine = Some(false),
        verified = Some(true))
      When("The info")
      When("Sending the request that updates the existing Product to the API")
      Patch(s"/api/review/${theNewRev.id}", f.review) ~> route ~> check {
        Then("The response status should be 200 OK")
        status.intValue() shouldBe 200
        And("Should return the updated Product")
        responseAs[Review] shouldBe f.review
      }
    }
  }

  Feature("Updating a Review's Product") {
    Scenario("As a Client I want to be able to update the product associated to a specific Review"){ f=>
      Given("Some existing Review")
      And("Another existing Product id")
      val newProd = Product("007", "Mistery box","NoCategory" )
      store ! CreateProduct(newProd)
      When("Executing the request to the backend API")
      Put(s"/api/review/${f.review.id}/updateProduct", newProd.id) ~> route ~> check {
        Then("The status code should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return the updated review")
        responseAs[Review].id shouldBe f.review.id
        responseAs[Review].product shouldBe newProd.id
      }
    }
  }
  Feature("Updating a Review's Customer") {
    Scenario("As a Client I want to be able to update the customer associated to a specific Review") { f=>
      Given("Some existing Review")
      And("Another existing Customer id")
      val newCustomer = Customer(112233L, "John Wick")
      store ! CreateCustomer(newCustomer)
      When("Executing the request to the backend API")
      Put(s"/api/review/${f.review.id}/updateCustomer", newCustomer.id.toString) ~> route ~> check {
        Then("The status code should be 200 OK")
        status.intValue() shouldBe 200
        And("It should return the updated review")
        responseAs[Review].id shouldBe f.review.id
        responseAs[Review].customer shouldBe newCustomer.id
      }
    }
  }

}

