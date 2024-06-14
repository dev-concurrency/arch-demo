package infrastructure
package persistence

import akka.Done
import akka.actor.*
import akka.actor.ExtendedActorSystem
import akka.serialization.*
import com.wallet.proto.messages.commands
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial

import WalletDataModel2.*

object ChimneyTransformers:

    transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues

    given fromProtoTo: Transformer[commands.CommandsADT, WalletCommands2.CommandsADT] with

        def transform(src: commands.CommandsADT): WalletCommands2.CommandsADT =
          src
            .intoPartial[WalletCommands2.CommandsADT]
            .withSealedSubtypeHandledPartial[commands.CommandsADT.Empty.type](
              _ => partial.Result.fromEmpty
            )
            .withSealedSubtypeHandled[commands.CommandsADT.NonEmpty](
              _.transformInto[WalletCommands2.CommandsADT]
            )
            .transform
            .asOption.get
        // def transform(src: commands.CommandsADT): WalletCommands2.CommandsADT =
        //   src
        //     .intoPartial[WalletCommands2.CommandsADT]
        //     .withEnumCaseHandledPartial[commands.CommandsADT.Empty.type](
        //       _ => partial.Result.fromEmpty
        //     )
        //     .withEnumCaseHandled[commands.CommandsADT.NonEmpty](
        //       _.transformInto[WalletCommands2.CommandsADT]
        //     )
        //     .transform

        //     .asOption.get

import ChimneyTransformers.given

import akka.actor.typed.scaladsl.adapter._

class MyOwnSerializer(system: ExtendedActorSystem) extends Serializer {

  private val actorRefResolver = ActorRefResolver(system.toTyped)

  // If you need logging here, introduce a constructor that takes an ExtendedActorSystem.
  // class MyOwnSerializer(actorSystem: ExtendedActorSystem) extends Serializer
  // Get a logger using:
  // private val logger = Logging(actorSystem, this)

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = true

  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 40 is reserved by Akka itself
  def identifier = 1234567

  // val id_ = "WalletCommands2.CommandsADT"

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    // Put the code that serializes the object here
    // #...

    obj match {
      case _: akka.Done  => commands.Done().toByteArray
      case _: OkResponse => commands.Done().toByteArray
      case d: Balance    => commands.Balance(d.value).toByteArray
      case d: Credit     =>
        // println(s"Converting to proto Credit: ${d.amount}")
        commands.Credit(d.amount).toByteArray
      case d: Debit      => commands.Debit(d.amount).toByteArray

      case FrameWorkCommands.CmdInst(x: WalletCommands2.CommandsADT, pmts: List[String], replyTo) =>
        println(s"Converting to proto CmdInst: $x")

        var y: WalletCommands2.CommandsADT = WalletCommands2.CommandsADT.StopCmd
        y = x.asInstanceOf[WalletCommands2.CommandsADT]

        // println(s"CmdInst converted: $y")

        val adt = y.transformInto[commands.CommandsADT].asMessage.toByteString
        val who = actorRefResolver.toSerializationFormat(replyTo) // .getBytes(StandardCharsets.UTF_8)

        commands.CmdInst(
          adt,
          pmts,
          who,
          classOf[WalletCommands2.CommandsADT].getCanonicalName
          // id_
        ).toByteArray
    }

    // Array[Byte]()

    // #...
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  def fromBinary(bytes: Array[Byte], clazz: Option[Class[?]]): AnyRef = {
    // Put your code that deserializes here
    // #...

    clazz match {
      case Some(c) if c == classOf[akka.Done] => Done

      case Some(c) if c == classOf[OkResponse] => OkResponse()

      case Some(c) if c == classOf[Balance] =>
        val d = commands.Balance.parseFrom(bytes)
        Balance(d.value)

      case Some(c) if c == classOf[Credit] =>
        val d = commands.Credit.parseFrom(bytes)

        // println(s"Converting from proto Credit: ${d.amount}")

        Credit(d.amount)

      case Some(c) if c == classOf[Debit]                                                =>
        val d = commands.Debit.parseFrom(bytes)
        Debit(d.amount)

      // case Some(c) if c == classOf[infrastructure.persistence.WalletCommands2.CmdInst[?, ?]] =>
      case Some(c) if c == classOf[infrastructure.persistence.FrameWorkCommands.CmdInst] =>
        val cmdInst = commands.CmdInst.parseFrom(bytes)
        val payload =
          if cmdInst.typeUrl == classOf[WalletCommands2.CommandsADT].getCanonicalName then

              val res = commands.CommandsADTMessage.parseFrom(cmdInst.payload.toByteArray).toCommandsADT.transformInto[WalletCommands2.CommandsADT]

              // println(s"Converting from proto CmdInst: ${res}")

              res
              // cmdInst.payload.transformInto[WalletCommands2.CommandsADT]
              // null
          else
              println(
                s"Unknown type: ${cmdInst.typeUrl} ========================================================================================================="
              )
              null

        val who = actorRefResolver.resolveActorRef(cmdInst.replyTo)
        FrameWorkCommands.CmdInst(payload, cmdInst.params.toList, who)

      case x =>
        println(s"${x} nuul =========================================================================================================")
        null
    }

    // null

    // #...
  }

}
