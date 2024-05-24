package components
package infrastructure
package wallet

import _root_.infrastructure.CborSerializable

object Events:
    sealed trait Event                       extends CborSerializable
    final case class CreditAdded(value: Int) extends Event
    final case class DebitAdded(value: Int)  extends Event
    final case class WalletCreated()         extends Event
