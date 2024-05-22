package event_sourcing
package examples

import cats.data.EitherT
import cats.effect.*
import com.example.*
import com.wallet.demo.clustering.grpc.admin.*
import io.grpc.*
import io.scalaland.chimney.dsl.*

class ClusteringWalletFs2GrpcServiceImpl[G: ExceptionGenerator](service: ClusteringWalletGrpcService[Result], transformers: MyTransformers[G])
    extends ClusteringWalletServiceFs2Grpc[cats.effect.IO, Metadata] {
  import transformers.othersTransformers

  def createWallet(request: RequestId, ctx: Metadata) = service.createWallet(request, ctx).transformInto[IO[Response]]
  def deleteWallet(request: RequestId, ctx: Metadata) = service.deleteWallet(request, ctx).transformInto[IO[Response]]
  def addCredit(request: CreditRequest, ctx: Metadata) = service.addCredit(request, ctx).transformInto[IO[Response]]
  def addDebit(request: DebitRequest, ctx: Metadata) = service.addDebit(request, ctx).transformInto[IO[Response]]
  def getBalance(request: RequestId, ctx: Metadata) = service.getBalance(request, ctx).transformInto[IO[BalanceResponse]]

  def operation(request: rpcOperationRequest, ctx: Metadata) = service.operation(request, ctx).transformInto[IO[rpcOperationResponse]]

}
