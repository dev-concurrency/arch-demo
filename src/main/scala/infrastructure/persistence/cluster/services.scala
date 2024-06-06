package event_sourcing
package examples

import infrastructure.persistence.*

import akka.Done
import fs2.concurrent.Channel
import infrastructure.persistence.WalletDataModel.*
import infrastructure.persistence.WalletDataModel2
import infrastructure.util.*

object WalletServices:

    trait WalletServiceIO[F[_]]:
        def createWallet(id: String): F[Done]
        def deleteWallet(id: String): F[Done]
        def addCredit(id: String, value: Credit): F[Done]
        def addDebit(id: String, value: Debit): F[Done]
        def getBalance(id: String): F[Balance]

    trait WalletServiceIO2[F[_]]:
        def createWallet(id: String): F[OkResponse]
        def deleteWallet(id: String): F[OkResponse]
        def addCredit(id: String, value: WalletDataModel2.Credit): F[OkResponse]
        def addDebit(id: String, value: WalletDataModel2.Debit): F[OkResponse]
        def getBalance(id: String): F[WalletDataModel2.Balance]

    trait WalletServiceIO_2[F[_]]:
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

    trait WalletService2:
        def createWallet(id: String): Future[OkResponse | ResultError]
        def deleteWallet(id: String): Future[OkResponse | ResultError]
        def addCredit(id: String, value: WalletDataModel2.Credit): Future[OkResponse | ResultError]
        def addDebit(id: String, value: WalletDataModel2.Debit): Future[OkResponse | ResultError]
        def getBalance(id: String): Future[WalletDataModel2.Balance | ResultError]
