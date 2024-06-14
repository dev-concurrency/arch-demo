package components
package infrastructure
package wallet2
package command_handlers

import _root_.infrastructure.persistence.OkResponse
import _root_.infrastructure.persistence.WalletCommands2.*
import _root_.infrastructure.persistence.WalletDataModel2.*
import _root_.infrastructure.persistence.WalletEvents.*
import _root_.infrastructure.persistence.WalletState.*
import components.infrastructure.wallet2.WalletContainer2 as obj

val OperationsHandler = obj.CommandHandler {
  case (s: State, CommandsADT.CreditCmd(Credit(amount))) => (CreditAdded(amount), OkResponse())
  case (s: State, CommandsADT.DebitCmd(Debit(amount)))   => (DebitAdded(amount), OkResponse())
}
