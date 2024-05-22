package event_sourcing
package examples

import akka.grpc.GrpcServiceException
import cats.data.EitherT
import cats.effect.*
import cats.implicits.*
import com.example.*
import com.google.rpc.Code
import com.wallet.demo.clustering.grpc.admin.*
import fs2.concurrent.Channel
import io.grpc.*
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*

trait ExceptionGenerator[F]:
    def generateException(msg: String): Throwable

object ExceptionGenerator:
    def apply[F](using obj: ExceptionGenerator[F]): ExceptionGenerator[F] = obj

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

    implicit def queueToResultTransformer: Transformer[IO[Either[Channel.Closed, Boolean]], Result[Boolean]] =
      new Transformer[IO[Either[Channel.Closed, Boolean]], Result[Boolean]]:
          def transform(result: IO[Either[Channel.Closed, Boolean]]): Result[Boolean] = {
            EitherT(result.map {
              case Right(value) => Right(value)
              case Left(error)  => Left(ErrorsBuilder.internalServerError("Closed channel"))
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

import cats.*

trait ClusteringWalletGrpcService[F[_]] {
  def createWallet(request: RequestId, ctx: Metadata): F[Response]
  def deleteWallet(request: RequestId, ctx: Metadata): F[Response]
  def addCredit(request: CreditRequest, ctx: Metadata): F[Response]
  def addDebit(request: DebitRequest, ctx: Metadata): F[Response]
  def getBalance(request: RequestId, ctx: Metadata): F[BalanceResponse]

  def operation(request: rpcOperationRequest, ctx: Metadata): F[rpcOperationResponse]

}
