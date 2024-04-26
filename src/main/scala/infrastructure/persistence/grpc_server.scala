package event_sourcing
package examples

import io.grpc.ServerServiceDefinition
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

import fs2.grpc.syntax.all.*

import cats.effect.*
import com.wallet.demo.clustering.grpc.admin.*

import akka.grpc.{ GrpcServiceException, Trailers }
import com.google.rpc.Code
import io.grpc.*

import cats.implicits.*
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.*

import cats.data.EitherT

import infrastructure.persistence.WalletDataModel

import com.example.*

trait ExceptionGenerator[F]:
    def generateException(msg: String): Throwable

object ExceptionGenerator:
    def apply[F](using obj: ExceptionGenerator[F]): ExceptionGenerator[F] = obj

import akka.grpc.GrpcServiceException

class MyTransformers[G: ExceptionGenerator]:

    import scala.reflect.*

    transparent inline given TransformerConfiguration[?] =
      TransformerConfiguration.default.enableDefaultValues.enableBeanSetters.enableBeanGetters.enableInheritedAccessors

    implicit def eitherToResultTransformers[A: ClassTag]: Transformer[IO[Either[Throwable, A]], Result[A]] =
      new Transformer[IO[Either[Throwable, A]], Result[A]]:
          def transform(result: IO[Either[Throwable, A]]): Result[A] = {
            EitherT(result.map {
              case Right(value) => Right(value)
              case Left(error)  => Left(ErrorsBuilder.internalServerError(error.getMessage))
            })
          }
          
    implicit def othersTransformers[A: ClassTag]: Transformer[Result[A], IO[A]] =
      new Transformer[Result[A], IO[A]]:
          def transform(result: Result[A]): IO[A] = {
            import com.example.*

            result.foldF(
              error =>
                IO.raiseError(
                  error match {

                    case e: ServiceUnavailable =>
                      val error = ServiceUnavailableError(e.code, e.title, e.message)
                      GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                    case e: Conflict =>
                      val error = ServiceUnavailableError(e.code, e.title, e.message)
                      GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                    case e: BadRequest =>
                      // val trailers = new Metadata()
                      // TODO: put the proto message in the trailers
                      // https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/errorhandling/DetailErrorSample.java#L82
                      // Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException(trailers)
                      val error = BadRequestError(e.code, e.title, e.message)
                      GrpcServiceException(Code.INVALID_ARGUMENT, e.message, Seq(error))

                    case e: NotFound =>
                      val error = NotFoundError(e.code, e.title, e.message)
                      GrpcServiceException(Code.NOT_FOUND, e.message, Seq(error))

                    case e: InternalServer =>
                      val error = InternalServerError(e.code, e.title, e.message)
                      GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                    case e: Unauthorized =>
                      val error = UnauthorizedError(e.code, e.title, e.message)
                      GrpcServiceException(Code.UNAUTHENTICATED, e.message, Seq(error))

                    case e: Forbidden =>
                      val error = ForbiddenError(e.code, e.title, e.message)
                      GrpcServiceException(Code.PERMISSION_DENIED, e.message, Seq(error))
                  }
                ),
              value => IO { value }
            )
          }

    implicit def optionTransformers[A: ClassTag]: Transformer[Option[A], A] =
      new Transformer[Option[A], A]:
          def transform(self: Option[A]): A =
            self match
              case Some(aValue) => aValue
              case None         =>
                val tName = summon[ClassTag[A]].runtimeClass.getSimpleName
                throw ExceptionGenerator[G].generateException(s"$tName value cannot be empty")

case class RequestId3(id: String) {
  require(id.nonEmpty, "id cannot be empty")
}

case class OperationRequest(id: String, amount: Int) {
  require(id.nonEmpty, "id cannot be empty")
  require(amount > 0, "amount must be greater than 0")
}

import com.example.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.instances.*

import cats.syntax.all.*

import cats.mtl.*
import cats.mtl.implicits.*

trait ClusteringWalletGrpcService[F[_]] {
  def createWallet(request: RequestId, ctx: Metadata): F[Response]
  def deleteWallet(request: RequestId, ctx: Metadata): F[Response]
  def addCredit(request: CreditRequest, ctx: Metadata): F[Response]
  def addDebit(request: DebitRequest, ctx: Metadata): F[Response]
  def getBalance(request: RequestId, ctx: Metadata): F[BalanceResponse]
}

class ClusteringWalletGrpcServiceImpl[F[_], G: ExceptionGenerator](service: WalletEventSourcing.WalletServiceIO[F], repo: WalletEventSourcing.WalletRepository[F])
  (using transformers: MyTransformers[G])(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
    extends ClusteringWalletGrpcService[F] {
  import transformers.optionTransformers

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
    } yield Response(Some(r.id))

  def deleteWallet(request: RequestId, ctx: Metadata): F[Response] =
    for {
      r <- validateNonNullId(request)
      _ <- service.deleteWallet(r.id)
      _ <- repo.deleteWallet(r.id)
    } yield Response(Some(r.id))

  def addCredit(request: CreditRequest, ctx: Metadata): F[Response] =
    for {
      r <- validateCreditRequest(request)
      res <- service.addCredit(r.id, WalletDataModel.Credit(r.amount))
    } yield Response(Some(r.id))

  def addDebit(request: DebitRequest, ctx: Metadata): F[Response] =
    for {
      r <- validateDebitRequest(request)
      res <- service.addDebit(r.id, WalletDataModel.Debit(r.amount))
    } yield Response(Some(r.id))

  def getBalance(request: RequestId, ctx: Metadata): F[BalanceResponse] =
    // BalanceResponse(100).pure[F]
    // MT.raiseError(ErrorsBuilder.notFoundError("Not found"))
    // FR.raise(ErrorsBuilder.badRequestError("bad request"))
    for {
      r <- validateNonNullId(request)
      res <- service.getBalance(r.id)
    } yield BalanceResponse(res.value)

}

class ClusteringWalletFs2GrpcServiceImpl[G: ExceptionGenerator](service: ClusteringWalletGrpcService[Result], transformers: MyTransformers[G])
    extends ClusteringWalletServiceFs2Grpc[IO, Metadata] {
  import transformers.othersTransformers

  def createWallet(request: RequestId, ctx: Metadata) = service.createWallet(request, ctx).transformInto[IO[Response]]
  def deleteWallet(request: RequestId, ctx: Metadata) = service.deleteWallet(request, ctx).transformInto[IO[Response]]
  def addCredit(request: CreditRequest, ctx: Metadata) = service.addCredit(request, ctx).transformInto[IO[Response]]
  def addDebit(request: DebitRequest, ctx: Metadata) = service.addDebit(request, ctx).transformInto[IO[Response]]
  def getBalance(request: RequestId, ctx: Metadata) = service.getBalance(request, ctx).transformInto[IO[BalanceResponse]]
}

class GrpcServerResource:

    def helloService[G: ExceptionGenerator](wService: WalletEventSourcing.WalletServiceIO[Result], repo: WalletEventSourcing.WalletRepository[Result]): Resource[IO, ServerServiceDefinition] = {

      val transformers = new MyTransformers
      val sImpl = new ClusteringWalletGrpcServiceImpl[Result, G](wService, repo)(using transformers)

      ClusteringWalletServiceFs2Grpc.bindServiceResource[IO](
        new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers)
      )
    }

    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] = NettyServerBuilder
      .forPort(8080)
      .addService(service)
      .addService(ProtoReflectionService.newInstance())
      .resource[F]
