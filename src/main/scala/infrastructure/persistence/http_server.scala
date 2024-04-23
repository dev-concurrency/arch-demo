package event_sourcing
package examples

import hello.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.syntax.all.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import smithy4s.http4s.SimpleRestJsonBuilder
import scala.concurrent.duration.*

import smithy4s.kinds.PolyFunction
import smithy4s.codecs.*

import akka.cluster.typed.*
import akka.actor.ActorSystem as UntypedActorSystem

import cats.data.EitherT

import smithy4s.Hints
import org.http4s.headers.Authorization
import smithy4s.http4s.ServerEndpointMiddleware
import java.util.concurrent.Executors

import com.example.*
import org.http4s.server.Server

class HttpServerResource(using ec: ExecutionContextExecutor):

    def helloService: Resource[IO, Server] =

        val service = new ServiceImpl

        val res = SmithyResource(service)
          .all
          .flatMap {
            routes =>
                val thePort = port"9000"
                val theHost = host"0.0.0.0"
                val message = s"Server started on: $theHost:$thePort, press enter to stop"
                EmberServerBuilder
                  .default[IO]
                  .withPort(thePort)
                  .withHost(theHost)
                  .withHttpApp(routes.orNotFound)
                  .withShutdownTimeout(1.second)
                  .build
                //   .productL(IO.println(message).toResource)
          }
        res
