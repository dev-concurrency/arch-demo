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

import infrastructure.util.*
import infrastructure.persistence.FrameWorkCommands.*

trait Container2[C >: FC <: ProtoSerializable, FC <: ProtoSerializable, E, S, FCR <: ProtoSerializable] {

  type ReplyEffect = dsl.ReplyEffect[E, Option[S]]
  type CommandsHandlerResponse = (E | EffectType, ProtoSerializable | ResultError)

  trait AppCommands:
      def interpret(input: (S, C)): CommandsHandlerResponse 

  trait AppEvents:
      def interpret(input: (S, E)): S

  final case class CommandHandler(handle: PartialFunction[(S, C), CommandsHandlerResponse])
  final case class EventHandler(handle: PartialFunction[(S, E), S])


  object AppCommands {
 
    final class Impl
      (
        handlers: Set[CommandHandler])(using entityNoun: String) extends AppCommands {

      override def interpret(cmd: (S, C)): CommandsHandlerResponse = {
        handlers.map(_.handle)
          .reduce(_ orElse _).lift(cmd) match {
            case Some(answer: CommandsHandlerResponse) => answer
            case None                      =>
                                            (EffectType.None,  ResultError(
                                                TransportError.NotFound,
                                                s"$entityNoun does not exists"
                                              ))

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
        }       
          else {
            cmd._1
          }
      }

    }

  }

  trait CommandApplier:
      def applyCommand(state: S, cmd: CmdInst)(using classTagE: ClassTag[E], logger: Logger): ReplyEffect

  trait EventApplier:
      def applyEvent(state: S, event: E): S

  trait Handler
      (
        appE: AppEvents,
        appC: AppCommands,
        ):

      val cApp =
        new CommandApplier:
            def applyCommand(state: S, cmd: CmdInst)(using classTagE: ClassTag[E], logger: Logger): ReplyEffect = 
              appC.interpret((state, cmd.payload.asInstanceOf[C])) match {
                  case (event: E, response: (ProtoSerializable | ResultError)) =>
                    Effect.persist[E, S](event).thenReply(cmd.replyTo)( _ => response).asInstanceOf[ReplyEffect]
                  case (EffectType.None, response) =>
                    Effect.reply[ProtoSerializable | ResultError, E, S](cmd.replyTo)(response).asInstanceOf[ReplyEffect]
                  case (EffectType.Stop, response) =>
                    Effect.stop[E, S]().thenReply(cmd.replyTo)( _ => response).asInstanceOf[ReplyEffect]
              }

      val eApp =
        new EventApplier:
            def applyEvent(state: S, event: E): S = appE.interpret((state, event))

  trait EntityConfig
    (
      handler: Handler,
      tagger: (Option[S], E) => Set[String],
      firstCommandHandler: FC => Either[ResultError, (E, FCR)],
      firstEventHandler: E => Option[S],
      check_if_C_is_FC: C => Option[FC])(using classTagC: ClassTag[C], entityNoun: String) {
    val cApp = handler.cApp
    val eApp = handler.eApp

    val typeKey: EntityTypeKey[CmdInst] = EntityTypeKey[CmdInst](entityNoun)

    def onFirstCommand(cmd: CmdInst): ReplyEffect = {
      check_if_C_is_FC(cmd.payload.asInstanceOf[C]) match
        case Some(e) =>
          firstCommandHandler(e) match
            case Right((value, res: FCR)) =>
              Effect.persist(value)
                .thenReply(cmd.replyTo)(
                  _ => res
                )
            case Left(value)         =>
              Effect
                .none
                .thenReply(cmd.replyTo)(
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
      println(s"onFirstEvent")
      firstEventHandler(event) match
        case Some(state) => state
        case _           => throw new IllegalStateException(s"Unexpected event [$event] in empty state")

    def apply(persistenceId: PersistenceId)(using classTagE: ClassTag[E], logger: Logger): Behavior[CmdInst] = {
      val factory: ActorContext[CmdInst] => Behavior[CmdInst] = {
        (context: ActorContext[CmdInst]) =>
          EventSourcedBehavior.withEnforcedReplies[CmdInst, E, Option[S]](
            persistenceId,
            None,
            (state: Option[S], cmd: CmdInst) =>
              state match {
                case None              => 

                  println(s"onFirstCommand")
                  
                  onFirstCommand(cmd)

                  
                case Some[S](state: S) => 
                  println(s"state:  $state")
                  cApp.applyCommand(state, cmd)
              },
            (state: Option[S], event: E) =>
              state match {
                case None        => Some[S](onFirstEvent(event))
                case Some(state) => Some(eApp.applyEvent(state, event))
              }
          ).withTaggerForState(tagger)
      }
      Behaviors.setup[CmdInst](factory)
    }

    def echo = "oye --------------------------------------"

  }

}
