package components
package infrastructure
package wallet2
package event_handlers

import components.infrastructure.wallet2.WalletContainer2 as obj

import _root_.infrastructure.persistence.WalletDataModel2.*
import _root_.infrastructure.persistence.WalletEvents.*
import _root_.infrastructure.persistence.WalletState.*


val MoneyMovementHandler = obj.EventHandler {
  case (s: State, CreditAdded(amount)) => 
    println("Credit added")
    s.copy(balance = s.balance + amount)
  case (s: State, DebitAdded(amount))  => 
    println("Debit added")
    s.copy(balance = s.balance - amount)
}