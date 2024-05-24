package components
package infrastructure
package wallet
package event_handlers

import Events.*
import DataModel.*

val CreatedHandler = WalletContainer.EventHandler {
  case (s: State, WalletCreated()) => s
}
