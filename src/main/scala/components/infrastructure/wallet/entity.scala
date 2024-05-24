package components
package infrastructure
package wallet

import _root_.infrastructure.util.*
import akka.Done

import Commands.*
import Events.*
import DataModel.*

object WalletContainer extends _root_.infrastructure.components.persistence.Container[Command, CreateWalletCmd, Event, State, Done]:

    val tagger: (Option[State], Event) => Set[String] = {
      case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
      case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
      case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")
    }

    val firstCommandHandler: CreateWalletCmd => Either[ResultError, (Event, akka.Done)] = {
      cmd =>
        Right((WalletCreated(), Done))
    }

    val firstEventHandler: Event => Option[State] = {
      case WalletCreated() => 
        Some(State())
      case _               => None
    }

    val check_if_C_is_FC: Command => Option[CreateWalletCmd] = {
      case c: CreateWalletCmd => Some(c)
      case _                  => None
    }
