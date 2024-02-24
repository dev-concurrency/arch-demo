package payments

import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

import scala.concurrent.duration.*

class OkLimitSimulation extends Simulation, HttpProtocol, Transfers:

      setUp(
        transfers_1
          .inject(constantUsersPerSec(5).during(4.minutes))
          .protocols(httpProtocol)
          .throttle(
            reachRps(3).in(10.seconds),
            holdFor(10.seconds),
            jumpToRps(5),
            holdFor(3.minutes),
            jumpToRps(4),
            holdFor(40.seconds)
          )
      )
        .assertions(
          details("transfers requests")
            .failedRequests
            .count
            .between(1, 10, inclusive = true)
        )

