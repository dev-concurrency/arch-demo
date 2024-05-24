package components
package infrastructure
package wallet

import _root_.infrastructure.CborSerializable
import _root_.infrastructure.util.*
import akka.Done

object Commands:

    import DataModel.*

    sealed trait Command extends CborSerializable:
        def replyTo: ActorRef[ResultError]

    final case class CreateWalletCmd(replyTo: ActorRef[Done | ResultError])        extends Command
    final case class CreditCmd(amount: Int, replyTo: ActorRef[Done | ResultError]) extends Command
    final case class DebitCmd(amount: Int, replyTo: ActorRef[Done | ResultError])  extends Command
    final case class GetBalanceCmd(replyTo: ActorRef[Balance | ResultError])       extends Command
