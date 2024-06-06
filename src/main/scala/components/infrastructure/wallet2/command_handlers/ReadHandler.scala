package components
package infrastructure
package wallet2
package command_handlers

import _root_.infrastructure.persistence.OkResponse
import components.infrastructure.wallet2.WalletContainer2 as obj

import _root_.infrastructure.persistence.WalletCommands2.*
import _root_.infrastructure.persistence.WalletDataModel2.*
import _root_.infrastructure.persistence.WalletEvents.*
import _root_.infrastructure.persistence.WalletState.*

import _root_.infrastructure.util.*

val ReadHandler = obj.CommandHandler {
  case (state: State, CommandsADT.GetBalanceCmd) =>
    (EffectType.None, Balance(state.balance))
}
