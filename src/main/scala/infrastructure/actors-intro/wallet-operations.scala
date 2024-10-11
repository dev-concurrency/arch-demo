package demo
package examples

def init =
    sys ! Root.CreateWallet
    for
      case (response: ActorRef[Wallet.Command]) <- sys ? (Root.GetWallet(_))
    do
        wallet = Some(response)

def load: Unit = for w <- wallet do w.tell(Wallet.Load)

def save: Unit = for w <- wallet do w ! Wallet.Save

def printBalance: Unit =
  for
      w <- wallet
      case (response: Wallet.BalanceResponse) <- w ? (Wallet.GetBalance(_))
  do
      println(s"Balance: ${response.balance}")

def addCredit(c: Int): Unit =
  for w <- wallet do
      println("Adding credit")
      w ! Wallet.Credit(c)

def addDebit(c: Int): Unit = for w <- wallet do w ! Wallet.Debit(c)


import akka.serialization.*

sealed trait Model extends CborSerializable

enum MeasureAverageBudget(val id: String) extends Model:
    case PerDay   extends MeasureAverageBudget("per_day")
    case PerWeek  extends MeasureAverageBudget("per_week")
    case PerMonth extends MeasureAverageBudget("per_month")

case class MeasureAverage(
    budget: MeasureAverageBudget,
    value: Int
) extends Model

def chs =
  // https://doc.akka.io/docs/akka/current/serialization.html#programmatic
  val serialization = SerializationExtension(sys)
  val original = MeasureAverage(MeasureAverageBudget.PerMonth, 110)
  val bytes = serialization.serialize(original).get
  val serializerId = serialization.findSerializerFor(original).identifier
  val manifest = Serializers.manifestFor(serialization.findSerializerFor(original), original)

  // Turn it back into an object
  val back = serialization.deserialize(bytes, serializerId, manifest).get
  print(s"Original: $original\n")
  print(s"Back: $back\n")