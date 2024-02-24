package payments

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CircuitBreakerSimulation extends Simulation, Headers, HttpProtocol:

  val feeder_server_errors = jsonFile("feeds_server_errors.json").random

  val transfers_errors_1 = scenario("transfers server errors")
    .feed(feeder_server_errors)
    .exec(
      http("transfers request 1")
        .post("/payments/transfer")
        .headers(sentHeaders)
        .body(
          StringBody(
            """
                   {
                    "amount": 4230,
                    "to": "#{to}"
                   }
               """)
        )
        .check(status.is(200))
    )
    .pause(6.seconds)
    .exec(
      http("transfers request 2")
        .post("/payments/transfer")
        .headers(sentHeaders)
        .body(
          StringBody(
            """
                   {
                    "amount": 4230,
                    "to": "#{to}"
                   }
               """)
        )
        .check(status.is(200)) // <7>
    )
  setUp(
    transfers_errors_1
      .inject(constantUsersPerSec(500).during(4.minutes))
      .protocols(httpProtocol)
      .throttle(
        reachRps(300).in(2.seconds),
        holdFor(10.seconds),
        jumpToRps(100),
        holdFor(1.minutes),
        jumpToRps(400),
        holdFor(3.minutes)
      )
  )

