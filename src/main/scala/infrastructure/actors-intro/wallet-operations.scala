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
