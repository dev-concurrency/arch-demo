package event_sourcing
package examples

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
import infrastructure.persistence.WalletDataModel
import java.util.UUID

object ServerModule:

    import com.wallet.demo.clustering.grpc.admin.*

    class gRPCServerImpl(walletService: WalletEventSourcing.WalletService)(using sys: ActorSystem[Nothing]) extends ClusteringWalletServicePowerApi:
        val logger: Logger = LoggerFactory.getLogger(this.getClass)
        given ec: ExecutionContextExecutor = sys.executionContext

        def createWallet(in: Request, metadata: Metadata): Future[Response] =
            val id = UUID.randomUUID().toString
            walletService.createWallet(id).map:
                case akka.Done => Response(id)
                case _         => throw StatusRuntimeException(Status.NOT_FOUND.withDescription("Wallet not found"))

        def addCredit(in: CreditRequest, metadata: Metadata): Future[Response] =
            walletService.addCredit(in.id, WalletDataModel.Credit(in.amount))
            Future:
                Response("Credit added")

        def addDebit(in: DebitRequest, metadata: Metadata): Future[Response] =
            walletService.addDebit(in.id, WalletDataModel.Debit(in.amount))
            Future:
                Response("Debit added")

        def getBalance(in: RequestId, metadata: Metadata): Future[BalanceResponse] = walletService.getBalance(in.id).map:
            case WalletDataModel.Balance(balance) => BalanceResponse(balance)
            case _                                => throw StatusRuntimeException(Status.NOT_FOUND.withDescription("Wallet not found"))

    class gRPCServerImplDummy(using sys: ActorSystem[Nothing]) extends ClusteringWalletServicePowerApi:
        val logger: Logger = LoggerFactory.getLogger(this.getClass)
        given ec: ExecutionContextExecutor = sys.executionContext

        def createWallet(in: Request, metadata: Metadata): Future[Response] = Future:
            Response("Wallet not created")

        def addCredit(in: CreditRequest, metadata: Metadata): Future[Response] = Future:
            Response("No Credit added")

        def addDebit(in: DebitRequest, metadata: Metadata): Future[Response] = Future:
            Response("No Debit added")

        def getBalance(in: RequestId, metadata: Metadata): Future[BalanceResponse] = Future:
            BalanceResponse(0)

    class gRPCApi:

        var binding: Option[ServerBinding] = None

        def init(w: WalletEventSourcing.WalletService)(using sys: ActorSystem[Nothing]): Unit =
            given ec: ExecutionContextExecutor = sys.executionContext
            Try {
              val walletService: PartialFunction[HttpRequest, Future[HttpResponse]] = ClusteringWalletServicePowerApiHandler.partial(
                new gRPCServerImpl(w)
              )
              val reflectionService = akka.grpc.scaladsl.ServerReflection.partial(List(ClusteringWalletService))
              val service = ServiceHandler.concatOrNotFound(walletService, reflectionService)
              Http().newServerAt("0.0.0.0", 8080).bind(service)
            }.toEither match
              case Right(b) =>
                b.onComplete:
                    case Success(value)     =>
                      println("gRPC Server started")
                      binding = Some(value)
                    case Failure(exception) =>
                      println("gRPC Server not started")
                      exception.printStackTrace()
              case Left(e)  => e.printStackTrace()

        def stop: Unit =
          binding match
            case Some(b) =>
              println("Terminating server...")
              b.terminate(1.seconds)
              binding = None
            case None    => println("Server not started")
