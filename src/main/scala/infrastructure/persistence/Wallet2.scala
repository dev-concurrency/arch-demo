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

import org.slf4j.{ Logger, LoggerFactory }
import scala.reflect.ClassTag
import com.journal.account.events.Event

object WalletDataModel2:
    sealed trait Model                    extends ProtoSerializable
    final case class Balance(value: Long) extends Model
    final case class Credit(amount: Int)  extends Model
    final case class Debit(amount: Int)   extends Model


import java.io.Serializable
case class OkResponse() extends Serializable, ProtoSerializable

object FrameWorkCommands:
    sealed trait Cmd extends ProtoSerializable:
        def replyTo: ActorRef[ResultError]

    case class CmdInst(payload: ProtoSerializable, params: List[String], replyTo: ActorRef[ProtoSerializable | ResultError]) extends Cmd
    
object WalletCommands2:

    import WalletDataModel2.*

    enum CommandsADT extends ProtoSerializable:
      case CreateWalletCmd
      case CreditCmd(value: Credit)
      case DebitCmd(value: Debit)
      case GetBalanceCmd
      case StopCmd

trait CommandsHandler2:
    this: WalletEntity2.State =>

    import WalletCommands2.*
    import WalletDataModel2.*
    import WalletEvents.*
    import FrameWorkCommands.*
    
    def apCmd(
      state: WalletEntity2.State,
      cmd: ProtoSerializable
    ) : (WalletEvents.Event | EffectType, ProtoSerializable | ResultError) = 
      cmd match{
        case CommandsADT.StopCmd =>
          (EffectType.Stop, OkResponse())
        case CommandsADT.CreditCmd(WalletDataModel2.Credit(amount)) =>
          (CreditAdded(amount), OkResponse())
        case CommandsADT.DebitCmd(Debit(amount)) =>
          (DebitAdded(amount), OkResponse())
        case CommandsADT.GetBalanceCmd =>
          (EffectType.None, Balance(state.balance))
        case CommandsADT.CreateWalletCmd =>
          (EffectType.None, ResultError(TransportError.BadRequest, "Wallet already exists"))
      }


    def applyCommand(cmd: Cmd)(using logger: Logger): WalletEntity2.ReplyEffect =
      cmd match
        case CmdInst(cmd, _, replyTo) =>
          apCmd(this, cmd) match
            case (event: WalletEvents.Event, response) =>
              Effect.persist(event).thenReply(replyTo)( _ => response)
            case (EffectType.None, response) =>
              Effect.reply(replyTo)(response)
            case (EffectType.Stop, response) =>
              Effect.stop().thenReply(replyTo)( _ => response)


trait EventsHandler2:
    this: WalletEntity2.State =>

    import WalletEvents.*

    def applyEvent(event: WalletEvents.Event): WalletEntity2.State =
      event match
        case CreditAdded(amount) =>
          copy(balance = balance + amount)
        case DebitAdded(amount)  => copy(balance = balance - amount)
        case WalletCreated()     =>
          this

object WalletEntity2:

    given logger: Logger = LoggerFactory.getLogger(getClass)

    export WalletDataModel.*
    export WalletCommands2.*
    export WalletEvents.*
    import FrameWorkCommands.*

    val typeKey: EntityTypeKey[Cmd] = EntityTypeKey[Cmd]("wallet")

    type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, Option[State]]

// format: off
    case class State(balance: Long = 0)
                     extends CborSerializable,
                              CommandsHandler2,
                              EventsHandler2
// format: on

    def onFirstCommand(cmd: Cmd): ReplyEffect =
      cmd match
        case CmdInst(CommandsADT.CreateWalletCmd, _, replyTo) =>
          Effect.persist(WalletCreated(
          ))
            .thenReply(replyTo)(
              _ => OkResponse()
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

    def apply(persistenceId: PersistenceId, adapter: AccountDetachedModelsAdapter): Behavior[Cmd] = Behaviors.setup[Cmd]:
        context =>
            EventSourcedBehavior.withEnforcedReplies[Cmd, Event, Option[State]](
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
