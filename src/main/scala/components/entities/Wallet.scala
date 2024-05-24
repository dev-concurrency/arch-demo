package components
package entities

import scala.reflect.ClassTag

import _root_.infrastructure.util.*
import components.infrastructure.wallet.Commands.*
import components.infrastructure.wallet.DataModel.*
import components.infrastructure.wallet.Events.*
import components.infrastructure.wallet.command_handlers.*
import components.infrastructure.wallet.event_handlers.*
import distage.plugins.PluginDef

import infrastructure.wallet.WalletContainer as obj

object WalletModule extends PluginDef {

  many[obj.CommandHandler]
    .add(
      ReadHandler
    )

  many[obj.CommandHandler]
    .add(
      OperationsHandler
    )

  many[obj.EventHandler]
    .add(
      CreatedHandler
    )

  many[obj.EventHandler]
    .add(
      MoneyMovementHandler
    )

  make[String].from("wallet")
  make[obj.AppCommands].from[obj.AppCommands.Impl]
  make[obj.AppEvents].from[obj.AppEvents.Impl]

  makeTrait[obj.Handler]
  makeTrait[obj.EntityConfig]

  make[(Option[State], Event) => Set[String]].from:
      obj.tagger

  make[CreateWalletCmd => Either[ResultError, (Event, akka.Done)]].from:
      obj.firstCommandHandler

  make[Event => Option[State]].from:
      obj.firstEventHandler

  make[Command => Option[CreateWalletCmd]].from:
      obj.check_if_C_is_FC

  make[ClassTag[Command]].from:
      ClassTag(classOf[Command])

}
