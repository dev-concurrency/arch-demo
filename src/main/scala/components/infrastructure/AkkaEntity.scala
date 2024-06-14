package infrastructure
package components
package persistence

import scala.reflect.ClassTag
import scala.reflect.Selectable.reflectiveSelectable

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl as dsl
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import org.slf4j.Logger

import util.*

// https://stackoverflow.com/questions/24719112/impose-more-than-one-generic-type-constraint-on-a-scala-type-parameter
trait Container[C >: FC <: { def replyTo: ActorRef[ResultError] }, FC <: { def replyTo: ActorRef[FCR | ResultError] }, E, S, FCR] {
  type ReplyEffect = dsl.ReplyEffect[E, Option[S]]

  trait AppCommands:
      def interpret(input: (S, C)): ReplyEffect

  trait AppEvents:
      def interpret(input: (S, E)): S

  final case class CommandHandler(handle: PartialFunction[(S, C), ReplyEffect])
  final case class EventHandler(handle: PartialFunction[(S, E), S])

  object AppCommands {

    final class Impl
      (
        handlers: Set[CommandHandler])(using entityNoun: String) extends AppCommands {

      override def interpret(cmd: (S, C)): ReplyEffect = {
        handlers.map(_.handle)
          .reduce(_ orElse _).lift(cmd) match {
            case Some(answer: ReplyEffect) => answer
            case None                      =>
              Effect
                .none
                .thenReply(cmd._2.replyTo)(
                  _ =>
                    ResultError(
                      TransportError.NotFound,
                      s"$entityNoun does not exists"
                    )
                )

          }
      }

    }

  }

  object AppEvents {

    final class Impl
      (
        handlers: Set[EventHandler]) extends AppEvents {

      override def interpret(cmd: (S, E)): S = {
        val res = handlers.map(_.handle).reduce(_ orElse _)
        if res.isDefinedAt(cmd) then {
          res(cmd)
        } else {
          cmd._1
        }
      }

    }

  }

  trait CommandApplier:
      def applyCommand(state: S, cmd: C)(using logger: Logger): ReplyEffect

  trait EventApplier:
      def applyEvent(state: S, event: E): S

  trait Handler
      (
        appE: AppEvents,
        appC: AppCommands):

      val cApp =
        new CommandApplier:
            def applyCommand(state: S, cmd: C)(using logger: Logger): ReplyEffect = appC.interpret((state, cmd))

      val eApp =
        new EventApplier:
            def applyEvent(state: S, event: E): S = appE.interpret((state, event))

  trait EntityConfig
    (
      handler: Handler,
      tagger: (Option[S], E) => Set[String],
      firstCommandHandler: FC => Either[ResultError, (E, FCR)],
      firstEventHandler: E => Option[S],
      check_if_C_is_FC: C => Option[FC])(using classTag: ClassTag[C], entityNoun: String) {
    val cApp = handler.cApp
    val eApp = handler.eApp

    val typeKey: EntityTypeKey[C] = EntityTypeKey[C](entityNoun)

    def onFirstCommand(cmd: FC): ReplyEffect = {
      check_if_C_is_FC(cmd) match
        case Some(e) =>
          firstCommandHandler(e) match
            case Right((value, res)) =>
              Effect.persist(value)
                .thenReply(e.replyTo)(
                  _ => res
                )
            case Left(value)         =>
              Effect
                .none
                .thenReply(e.replyTo)(
                  _ =>
                    value
                )
        case None    =>
          Effect
            .none
            .thenReply(cmd.replyTo)(
              _ =>
                ResultError(
                  TransportError.NotFound,
                  s"$entityNoun does not exists"
                )
            )
    }

    def onFirstEvent(event: E): S =
      firstEventHandler(event) match
        case Some(state) => state
        case _           => throw new IllegalStateException(s"Unexpected event [$event] in empty state")

    def apply(persistenceId: PersistenceId)(using logger: Logger): Behavior[C] = {
      val factory: ActorContext[C] => Behavior[C] = {
        (context: ActorContext[C]) =>
          EventSourcedBehavior.withEnforcedReplies[C, E, Option[S]](
            persistenceId,
            None,
            (state: Option[S], cmd: C) =>
              state match {
                case None              =>
                  // TODO: traceId set point
                  onFirstCommand(cmd.asInstanceOf[FC])
                case Some[S](state: S) =>
                  // TODO: traceId set point
                  cApp.applyCommand(state, cmd)
              },
            (state: Option[S], event: E) =>
              state match {
                case None        => Some[S](onFirstEvent(event))
                case Some(state) => Some(eApp.applyEvent(state, event))
              }
          ).withTaggerForState(tagger)
      }
      Behaviors.setup[C](factory)
    }

    def echo = "oye"

  }

}

// import infrastructure.persistence.WalletDataModel.*
// import infrastructure.persistence.WalletCommands.*
// import infrastructure.persistence.WalletEvents.*

// case class State(balance: Long = 0) extends CborSerializable

// class WalletEntity extends Container[Command, CreateWalletCmd, Event, State, Done] {}

// extension [A](a: A)
//     def |>[B](f: (A) => B): B = f(a)
//     def |[B](f: (A) => B): B = a.pipe(f)

/*

type TContainer = Container[Command, CreateWalletCmd, Event, State, Done]
object obj extends TContainer

object we:
    val operationsHandler = obj.CommandHandler {
      case (s: State, CreditCmd(amount, replyTo)) =>
        Effect.persist(CreditAdded(amount)).thenReply(replyTo)(
          _ => Done
        )

      case (s: State, DebitCmd(amount, replyTo)) =>
        Effect.persist(DebitAdded(amount)).thenReply(replyTo)(
          _ => Done
        )
    }

    val readHandler = obj.CommandHandler {
      case (s: State, GetBalanceCmd(replyTo)) =>
        Effect.reply(replyTo)(
          Balance(
            s.balance
          )
        )
    }

    val eventWCreatedHandler = obj.EventHandler {
      case (s: State, WalletCreated()) => s
    }

    val eventMoneyMovementHandler = obj.EventHandler {
      case (s: State, CreditAdded(amount)) => s.copy(balance = s.balance + amount)
      case (s: State, DebitAdded(amount))  => s.copy(balance = s.balance - amount)
    }

    val tagger: (Option[State], Event) => Set[String] = {
      case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
      case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
      case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")
    }

    val firstCommandHandler: CreateWalletCmd => Either[ResultError, (Event, akka.Done)] = {
      cmd =>
        Right((WalletCreated(), Done))
    }

    val firstEventHandler: Event => Option[State] = {
      case WalletCreated() => Some(State())
      case _               => None
    }

    val check_if_C_is_FC: Command => Option[CreateWalletCmd] = {
      case c: CreateWalletCmd => Some(c)
      case _                  => None
    }

object AppModule extends PluginDef {

  many[obj.CommandHandler]
    .add(
      we.readHandler
    )

  many[obj.CommandHandler]
    .add(
      we.operationsHandler
    )

  many[obj.EventHandler]
    .add(
      we.eventWCreatedHandler
    )

  many[obj.EventHandler]
    .add(
      we.eventMoneyMovementHandler
    )

  make[String].from("wallet")
  make[obj.AppCommands].from[obj.AppCommands.Impl]
  make[obj.AppEvents].from[obj.AppEvents.Impl]

  make[State].from:
      State()

  makeTrait[obj.Handler]
  makeTrait[obj.EntityConfig]

  make[(Option[State], Event) => Set[String]].from:
      we.tagger

  make[CreateWalletCmd => Either[ResultError, (Event, akka.Done)]].from:
      we.firstCommandHandler

  make[Event => Option[State]].from:
      we.firstEventHandler

  make[Command => Option[CreateWalletCmd]].from:
      we.check_if_C_is_FC

  make[ClassTag[Command]].from:
      ClassTag(classOf[Command])

}

def runDemo =
    val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("infrastructure.components.persistence"))
    val appModules = PluginLoader().load(pluginConfig)
    val module = appModules.result.merge

    val app = Injector().produceGet[obj.EntityConfig](module).unsafeGet()
    println(app.echo)

@main
def run = runDemo

 */
