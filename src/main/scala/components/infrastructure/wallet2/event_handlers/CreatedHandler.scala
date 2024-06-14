package components
package infrastructure
package wallet2
package event_handlers

import _root_.infrastructure.persistence.WalletEvents.*
import _root_.infrastructure.persistence.WalletState.*
import components.infrastructure.wallet2.WalletContainer2 as obj

val CreatedHandler = obj.EventHandler {
  case (s: State, WalletCreated()) =>
    println("Wallet created")
    s
}
