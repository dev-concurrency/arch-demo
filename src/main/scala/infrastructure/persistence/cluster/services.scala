package event_sourcing
package examples

import akka.Done
import fs2.concurrent.Channel
import infrastructure.persistence.WalletDataModel.*
import infrastructure.util.*

object WalletServices:

    trait WalletServiceIO[F[_]]:
        def createWallet(id: String): F[Done]
        def deleteWallet(id: String): F[Done]
        def addCredit(id: String, value: Credit): F[Done]
        def addDebit(id: String, value: Debit): F[Done]
        def getBalance(id: String): F[Balance]

    trait WalletServiceIO2[F[_]]:
        def createWallet(id: String): F[Done]
        def deleteWallet(id: String): F[Done]

    trait WalletRepository[F[_]]:
        def deleteWallet(id: String): F[Int]

    trait WalletQueue[F[_]]:
        def trySend(data: ProducerParams): F[Boolean]

    trait WalletRepositoryIO[F[_]]:
        def deleteWallet(id: String): F[Either[Throwable, Int]]

    trait WalletQueueIO[F[_]]:
        def trySend(data: ProducerParams): F[Either[Channel.Closed, Boolean]]

    trait WalletService:
        def createWallet(id: String): Future[Done | ResultError]
        def deleteWallet(id: String): Future[Done | ResultError]
        def addCredit(id: String, value: Credit): Future[Done | ResultError]
        def addDebit(id: String, value: Debit): Future[Done | ResultError]
        def getBalance(id: String): Future[Balance | ResultError]
