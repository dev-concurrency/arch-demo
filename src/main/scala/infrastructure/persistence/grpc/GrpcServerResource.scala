package event_sourcing
package examples

import cats.data.EitherT
import cats.effect.*
import com.example.*
import com.wallet.demo.clustering.grpc.admin.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
// import io.scalaland.chimney.*

class GrpcServerResource:

    def helloService[G: ExceptionGenerator]
      (
        wService: WalletEventSourcing.WalletServiceIO[Result] | WalletEventSourcing.WalletServiceIO2[Result],
        repo: WalletEventSourcing.WalletRepository[Result])
      : Resource[IO, ServerServiceDefinition] = {

      val transformers = new MyTransformers
      val sImpl = new ClusteringWalletGrpcServiceImpl[Result, G](wService, repo)(using transformers)

      ClusteringWalletServiceFs2Grpc.bindServiceResource[cats.effect.IO](
        new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers)
      )
    }

    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] = NettyServerBuilder
      .forPort(8080)
      .addService(service)
      .addService(ProtoReflectionService.newInstance())
      .resource[F]
