package event_sourcing
package examples

import cats.implicits.*
import com.wallet.demo.clustering.grpc.admin.*
import io.scalaland.chimney.dsl.*

object Dto:

    enum AdSetup:
        case Case1(msg: String)
        case Case2(value: Int)

// import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.{ partial, Transformer }

object ChimneyTransformers:

    transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues

    given fromProtoAdSetupToAdSetup: Transformer[AdSetup, Dto.AdSetup] with

        def transform(src: AdSetup): Dto.AdSetup =
          src
            .intoPartial[Dto.AdSetup]
            .withCoproductInstancePartial[AdSetup.Empty.type](
              _ => partial.Result.fromEmpty
            )
            .withCoproductInstance[AdSetup.NonEmpty](
              _.transformInto[Dto.AdSetup]
            )
            .transform
            .asOption.get
