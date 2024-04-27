package event_sourcing
package examples

import scala.concurrent.duration.*

import cats.effect.*
import com.comcast.ip4s.*
import com.example.*
import org.http4s.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
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
                s"Server started on: $theHost:$thePort, press enter to stop"
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
