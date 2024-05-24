package components
package infrastructure
package wallet
package command_handlers

import akka.Done
import akka.persistence.typed.scaladsl.Effect

import Commands.*
import Events.*
import DataModel.*

val OperationsHandler = WalletContainer.CommandHandler {
  case (s: State, CreditCmd(amount, replyTo)) =>
    Effect.persist(CreditAdded(amount)).thenReply(replyTo)(
      _ => Done
    )

  case (s: State, DebitCmd(amount, replyTo)) =>
    Effect.persist(DebitAdded(amount)).thenReply(replyTo)(
      _ => Done
    )
}
