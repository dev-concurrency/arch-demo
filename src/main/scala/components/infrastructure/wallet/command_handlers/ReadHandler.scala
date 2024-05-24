package components
package infrastructure
package wallet
package command_handlers

import akka.persistence.typed.scaladsl.Effect

import Commands.*
import DataModel.*

val ReadHandler = WalletContainer.CommandHandler {
  case (s: State, GetBalanceCmd(replyTo)) =>
    Effect.reply(replyTo)(
      Balance(
        s.balance
      )
    )
}
