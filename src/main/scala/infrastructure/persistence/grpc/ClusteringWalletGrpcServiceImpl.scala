package event_sourcing
package examples

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.mtl.*
import com.example.*
import com.wallet.demo.clustering.grpc.admin.*
import infrastructure.persistence.WalletDataModel
import io.grpc.*
import io.scalaland.chimney.dsl.*

class ClusteringWalletGrpcServiceImpl[F[_], G: ExceptionGenerator]
  (service: WalletEventSourcing.WalletServiceIO[F], repo: WalletEventSourcing.WalletRepository[F])(using transformers: MyTransformers[G])
  (using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
    extends ClusteringWalletGrpcService[F] {

  def cha(id3: RequestId3): F[Unit] =
    if id3.id.startsWith("x") then
        FR.raise(ErrorsBuilder.forbiddenError("id cannot start with x"))
    else
        F.pure(())

  def chb(request: RequestId): F[RequestId3] = {
    val res = Try(request.transformInto[RequestId3]).toEither
    res match {
      case Right(r) => r.pure[F]
      case Left(e)  => FR.raise(ErrorsBuilder.badRequestError(e.getMessage))
    }
  }

  // def createWallet(request: RequestId, ctx: Metadata): F[Response] =
  //   for {
  //     r <- chb(request)
  //     _ <- cha(r)
  //   } yield Response(r.id)
  // def deleteWallet(request: RequestId, ctx: Metadata): F[Response] = ??? // IO(Response("x"))

  private def validateNonNullId(request: RequestId): F[RequestId3] =
      val res = Try(request.transformInto[RequestId3]).toEither
      res match
        case Right(r) => r.pure[F]
        case Left(e)  => FR.raise(ErrorsBuilder.badRequestError(e.getMessage))

  private def validateCreditRequest(request: CreditRequest): F[OperationRequest] =
      val res = Try(request.transformInto[OperationRequest]).toEither
      res match
        case Right(r) => r.pure[F]
        case Left(e)  => FR.raise(ErrorsBuilder.badRequestError(e.getMessage))

  private def validateDebitRequest(request: DebitRequest): F[OperationRequest] =
      val res = Try(request.transformInto[OperationRequest]).toEither
      res match
        case Right(r) => r.pure[F]
        case Left(e)  => FR.raise(ErrorsBuilder.badRequestError(e.getMessage))

  def createWallet(request: RequestId, ctx: Metadata): F[Response] =
    for {
      r <- validateNonNullId(request)
      res <- service.createWallet(r.id)
    } yield Response(r.id)

  def deleteWallet(request: RequestId, ctx: Metadata): F[Response] =
    for {
      r <- validateNonNullId(request)
      _ <- service.deleteWallet(r.id)
      _ <- repo.deleteWallet(r.id)
    } yield Response(r.id)

  def addCredit(request: CreditRequest, ctx: Metadata): F[Response] =
    for {
      r <- validateCreditRequest(request)
      res <- service.addCredit(r.id, WalletDataModel.Credit(r.amount))
    } yield Response(r.id)

  def addDebit(request: DebitRequest, ctx: Metadata): F[Response] =
    for {
      r <- validateDebitRequest(request)
      res <- service.addDebit(r.id, WalletDataModel.Debit(r.amount))
    } yield Response(r.id)

  val hkey = Metadata.Key.of("header_key", Metadata.ASCII_STRING_MARSHALLER)

  def getBalance(request: RequestId, ctx: Metadata): F[BalanceResponse] =
      // BalanceResponse(100).pure[F]
      // MT.raiseError(ErrorsBuilder.notFoundError("Not found"))
      // FR.raise(ErrorsBuilder.badRequestError("bad request"))

      // for {
      //   r <- validateNonNullId(request)
      //   res <- service.getBalance(r.id)
      // } yield BalanceResponse(res.value)

      println(ctx.get(hkey))

      for {
        value <- F.pure(600)
      } yield BalanceResponse(value)

  def operation(request: rpcOperationRequest, ctx: Metadata): F[rpcOperationResponse] =
    // import ChimneyTransformers.given

    // println(request.in.transformInto[Dto.AdSetup])

    F.pure(rpcOperationResponse(333))

}
