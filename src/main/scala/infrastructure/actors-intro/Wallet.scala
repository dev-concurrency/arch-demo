package demo

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors

import org.json4s.given
import org.json4s.{ DefaultFormats, Formats }
import org.json4s.native.JsonMethods
import org.json4s.native.Serialization

object Root:

    trait Command
    final case class GetWallet(replyTo: ActorRef[ActorRef[Wallet.Command]]) extends Command
    case object CreateWallet                                                extends Command

    def apply
      (
        state: Option[ActorRef[Wallet.Command]] = None)
      : Behavior[Command] = Behaviors.receive {

      (context, message) =>

        (message, state) match

          case (CreateWallet, None) =>
            context.log.info(s"Creating wallet")
            val actor = context.spawnAnonymous(Wallet.apply(Wallet.State()))
            apply(Some(actor))

          case (GetWallet(replyTo), Some(wallet)) =>
            replyTo ! wallet
            Behaviors.same

          case _ => // non intelligent compiler
            context.log.info(s"Default case")
            Behaviors.same

    }

object Wallet:

    case class BalanceResponse(balance: Int)

    case class State(credits: List[Credit] = List(), debits: List[Debit] = List()):

        def balance: Int =
          credits.map(
            credit => credit.amount
          ).sum - debits.map(
            debit => debit.amount
          ).sum

    trait Command

    case class Credit(amount: Int)                            extends Command
    case class Debit(amount: Int)                             extends Command
    case class GetBalance(replyTo: ActorRef[BalanceResponse]) extends Command
    object Load                                               extends Command
    object Save                                               extends Command

    @annotation.nowarn("msg=.*Compiler synthesis of Manifest and OptManifest is deprecated.*")
    def apply(state: State): Behavior[Command] = Behaviors.receive {
      (context, message) =>
        message match

          case p @ Credit(_) => apply(state.copy(credits = p :: state.credits))

          case p @ Debit(_) => apply(state.copy(debits = p :: state.debits))

          case GetBalance(replyTo) =>
            // context.log.info(s"Actual value: ${state.balance}")

            replyTo ! BalanceResponse(state.balance)

            Behaviors.same

          case Load =>
            val r = requests.get("http://localhost:8000/api/v1/state")
            given formats: Formats = DefaultFormats
            val json = JsonMethods.parse(r.text())
            apply(json.extract[State])

          case Save =>
            given formats: Formats = DefaultFormats
            import requests.RequestBlob
            val s = Serialization.write(state)
            context.log.info(s"Saving state: $s")
            requests.post(
              "http://localhost:8000/api/v1/state",
              data = RequestBlob.ByteSourceRequestBlob(s),
              // proxy=("localhost", 8080),
              headers = Map("Content-Type" -> "application/json"),
            )
            Behaviors.same

    }
