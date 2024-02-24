package payments

import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

trait Transfers extends Headers {

  val transfers_1 = scenario("transfers ok") // A scenario is a chain of requests and pauses
    .exec(
      http("transfers requests") // Here's an example of a POST request
        .post("/payments/transfer")
        .headers(sentHeaders)
        //            .header("X-Reference-Id", "${id}")
        .body(
          StringBody(
            """
                   {
                    "amount": 4230,
                    "to": "2325215132",
                    "currency": "UGX"
                   }
               """)
        )
        .check(status.is(200))
    )


}
