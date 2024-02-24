package demo

import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.actor.typed.ActorSystem

import scala.concurrent.{ ExecutionContextExecutor, Future }
import io.grpc.Status
import akka.actor.ActorSystem as ClassicActorSystem
import org.slf4j.{ Logger, LoggerFactory }
import io.grpc.StatusRuntimeException

import akka.grpc.scaladsl.Metadata

import akka.grpc.scaladsl.ServiceHandler

object ServerModule:

    import com.wallet.demo.grpc.admin.*

    class ServerImpl(walletRef: ActorRef[Wallet.Command]) extends WalletServicePowerApi:
        val logger: Logger = LoggerFactory.getLogger(classOf[ServerImpl])

        def addCredit(in: CreditRequest, metadata: Metadata): Future[Response] =
            walletRef ! Wallet.Credit(in.amount)
            Future:
                Response("Credit added")

        def addDebit(in: DebitRequest, metadata: Metadata): Future[Response] =
            walletRef ! Wallet.Debit(in.amount)
            Future:
                Response("Debit added")

        def getBalance(in: Request, metadata: Metadata): Future[BalanceResponse] = (walletRef ? (Wallet.GetBalance(_)))
          .mapTo[Wallet.BalanceResponse]
          .map:
              response => BalanceResponse(response.balance)

    class ServerDummyImpl extends WalletServicePowerApi:
        val logger: Logger = LoggerFactory.getLogger(classOf[ServerImpl])

        def addCredit(in: CreditRequest, metadata: Metadata): Future[Response] = Future:
            Response("Credit added")

        def addDebit(in: DebitRequest, metadata: Metadata): Future[Response] = Future:
            Response("Debit added")

        def getBalance(in: Request, metadata: Metadata): Future[BalanceResponse] = Future:
            BalanceResponse(5)

    object Server:

        def init(): Either[Throwable, Option[Future[ServerBinding]]] =
          Try {
            wallet.map:
                case w =>
                  val walletService: PartialFunction[HttpRequest, Future[HttpResponse]] = WalletServicePowerApiHandler.partial(new ServerImpl(w))
                  val reflectionService = akka.grpc.scaladsl.ServerReflection.partial(List(WalletService))
                  val service = ServiceHandler.concatOrNotFound(walletService, reflectionService)
                  Http().newServerAt("0.0.0.0", 8080).bind(service)
          }.toEither
