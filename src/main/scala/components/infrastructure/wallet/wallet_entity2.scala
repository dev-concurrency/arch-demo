package components
package infrastructure
package wallet2

import _root_.infrastructure.util.*
import _root_.infrastructure.persistence.OkResponse

import _root_.infrastructure.persistence.WalletCommands2.*
import _root_.infrastructure.persistence.WalletDataModel2.*
import _root_.infrastructure.persistence.WalletEvents.*
import _root_.infrastructure.persistence.WalletState.*

// import Commands.*
// import Events.*
// import DataModel.*

object WalletContainer2 extends _root_.infrastructure.components.persistence.Container2[CommandsADT, CommandsADT.CreateWalletCmd.type, Event, State, OkResponse]:

    val tagger: (Option[State], Event) => Set[String] = {
      case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
      case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
      case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")
    }

    val firstCommandHandler: CommandsADT => Either[ResultError, (Event, OkResponse)] = {
      cmd =>
        cmd match{
            case CommandsADT.CreateWalletCmd => 
                    Right((WalletCreated(), OkResponse()))
            case _ => Left(ResultError(TransportError.NotFound, "Wallet does not exists"))
        }
    }

    val firstEventHandler: Event => Option[State] = {
      case WalletCreated() => 
        Some(State())
      case _               => None
    }

    val check_if_C_is_FC: CommandsADT => Option[CommandsADT.CreateWalletCmd.type] = {
      case CommandsADT.CreateWalletCmd => Some(CommandsADT.CreateWalletCmd)
      case _                  => None
    }
