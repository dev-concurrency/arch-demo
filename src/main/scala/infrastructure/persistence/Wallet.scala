package infrastructure
package persistence

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import infrastructure.util.*

import scala.reflect.Selectable.reflectiveSelectable

object WalletDataModel:
    sealed trait Model                    extends CborSerializable
    final case class Balance(value: Long) extends Model
    final case class Credit(amount: Int)  extends Model
    final case class Debit(amount: Int)   extends Model

object WalletState:
  case class State(balance: Long = 0) extends CborSerializable
    
object WalletCommands:

    import WalletDataModel.*

    trait ReplyAble:
        def replyTo: ActorRef[ResultError]

    sealed trait Command extends CborSerializable:
        def replyTo: ActorRef[ResultError]

    final case class CreateWalletCmd(replyTo: ActorRef[Done | ResultError])        extends Command
    final case class CreditCmd(amount: Int, replyTo: ActorRef[Done | ResultError]) extends Command
    final case class DebitCmd(amount: Int, replyTo: ActorRef[Done | ResultError])  extends Command
    final case class GetBalanceCmd(replyTo: ActorRef[Balance | ResultError])       extends Command
    final case class StopCmd(replyTo: ActorRef[Done | ResultError])                extends Command


object WalletEvents:
    sealed trait Event                       extends CborSerializable
    final case class CreditAdded(value: Int) extends Event
    final case class DebitAdded(value: Int)  extends Event
    final case class WalletCreated()         extends Event

import org.slf4j.{ Logger, LoggerFactory }

trait CommandsHandler:
    this: WalletEntity.State =>

    import WalletCommands.*
    import WalletDataModel.*
    import WalletEvents.*

    def applyCommand(cmd: Command)(using logger: Logger): WalletEntity.ReplyEffect =
      cmd match

        case StopCmd(replyTo) =>
          Effect.stop().thenReply(replyTo)(
            _ =>
              Done
          )

        case CreditCmd(amount, replyTo) =>
          Effect.persist(CreditAdded(amount)).thenReply(replyTo)(
            _ => Done
          )

        case DebitCmd(amount, replyTo) =>
          Effect.persist(DebitAdded(amount)).thenReply(replyTo)(
            _ => Done
          )

        case GetBalanceCmd(replyTo) =>
          Effect.reply(replyTo)(
            Balance(
              balance
            )
          )

        case CreateWalletCmd(replyTo) =>
          Effect.reply(replyTo)(
            ResultError(
              TransportError.BadRequest,
              "Wallet already exists"
            )
          )

trait EventsHandler:
    this: WalletEntity.State =>

    import WalletEvents.*

    def applyEvent(event: Event): WalletEntity.State =
      event match
        case CreditAdded(amount) =>
          // println("Credit added =============")
          copy(balance = balance + amount)
        case DebitAdded(amount)  => copy(balance = balance - amount)
        case WalletCreated()     =>
          // println("Wallet created =============")
          this

object WalletEntity:

    given logger: Logger = LoggerFactory.getLogger(getClass)

    export WalletDataModel.*
    export WalletCommands.*
    export WalletEvents.*

    val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("wallet")

    type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, Option[State]]

// format: off
    case class State(balance: Long = 0)
                     extends CborSerializable,
                              CommandsHandler,
                              EventsHandler
// format: on

    def onFirstCommand(cmd: Command): ReplyEffect =
      cmd match
        case CreateWalletCmd(replyTo) =>
          Effect.persist(WalletCreated(
          ))
            .thenReply(replyTo)(
              _ => Done
            )
        case default                  =>
          Effect
            .none
            .thenReply(default.replyTo)(
              _ =>
                ResultError(
                  TransportError.NotFound,
                  "Wallet does not exists"
                )
            )

    def onFirstEvent(event: Event): State =
      event match
        case WalletCreated() => State()
        case _               => throw new IllegalStateException(s"unexpected event [$event] in empty state")

    def apply(persistenceId: PersistenceId, adapter: AccountDetachedModelsAdapter): Behavior[Command] = Behaviors.setup[Command]:
        context =>
            EventSourcedBehavior.withEnforcedReplies[Command, Event, Option[State]](
              persistenceId,
              None,
              (state, cmd) =>
                state match {
                  case None        => onFirstCommand(cmd)
                  case Some(state) => state.applyCommand(cmd)
                },
              (state, event) =>
                state match {
                  case None        => Some(onFirstEvent(event))
                  case Some(state) =>
                    println(s"Event applied: $event")
                    Some(state.applyEvent(event))
                }
            )
              .withTaggerForState:
                  case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
                  case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
                  case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")
        // .eventAdapter(adapter)
