package demo
package examples

import akka.http.scaladsl.Http.ServerBinding

object ServerMain:
    import ServerModule.Server

    def gRPCServerStart: Unit =
      Server.init() match
        case Left(value)  => value.printStackTrace()
        case Right(value) =>
          value match
            case None    => println("Server not started")
            case Some(b) =>
              b.onComplete:
                  case Success(value)     =>
                    println("Server started...")
                    binding = Some(value)
                  case Failure(exception) =>
                    println("Server not started")
                    exception.printStackTrace()

    def gRPCServerStop: Unit =
      for b <- binding do
          // println("Unbinding...")
          // b.unbind() // do not allow new connections
          println("Terminating server...")
          b.terminate(1.seconds)
          binding = None
