package components
package infrastructure
package wallet

import _root_.infrastructure.CborSerializable

object DataModel:
    sealed trait Model                    extends CborSerializable
    final case class Balance(value: Long) extends Model
    final case class Credit(amount: Int)  extends Model
    final case class Debit(amount: Int)   extends Model

    case class State(balance: Long = 0) extends CborSerializable
