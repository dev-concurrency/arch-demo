package components
package infrastructure
package wallet
package event_handlers

import Events.*
import DataModel.*

val MoneyMovementHandler = WalletContainer.EventHandler {
  case (s: State, CreditAdded(amount)) => s.copy(balance = s.balance + amount)
  case (s: State, DebitAdded(amount))  => s.copy(balance = s.balance - amount)
}
