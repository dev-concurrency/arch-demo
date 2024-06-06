package components
package entities2

import scala.reflect.ClassTag

import _root_.infrastructure.util.*

// import components.infrastructure.wallet.Commands.*
import _root_.infrastructure.persistence.WalletCommands2.*

// import components.infrastructure.wallet.DataModel.*
import _root_.infrastructure.persistence.WalletDataModel2.*

// import components.infrastructure.wallet.Events.*
import _root_.infrastructure.persistence.WalletEvents.*

// import components.infrastructure.wallet.command_handlers.*
import components.infrastructure.wallet2.command_handlers.*

// import components.infrastructure.wallet.event_handlers.*
import components.infrastructure.wallet2.event_handlers.*

import _root_.infrastructure.persistence.WalletState.*

import distage.plugins.PluginDef

// import infrastructure.wallet.WalletContainer as obj
import components.infrastructure.wallet2.WalletContainer2 as obj
import _root_.infrastructure.persistence.OkResponse

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

  make[CommandsADT.CreateWalletCmd.type => Either[ResultError, (Event, OkResponse)]].from:
      obj.firstCommandHandler

  make[Event => Option[State]].from:
      obj.firstEventHandler

  make[CommandsADT => Option[CommandsADT.CreateWalletCmd.type]].from:
      obj.check_if_C_is_FC

  make[ClassTag[CommandsADT]].from:
      ClassTag(classOf[CommandsADT])

  make[ClassTag[Event]].from:
      ClassTag(classOf[Event])

}
