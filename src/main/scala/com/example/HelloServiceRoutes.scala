package com.example

import hello.*
import cats.effect.*
import org.http4s.*
import smithy4s.http4s.SimpleRestJsonBuilder

import smithy4s.kinds.PolyFunction
import smithy4s.codecs.*


import cats.data.EitherT

import smithy4s.Hints
import smithy4s.http4s.ServerEndpointMiddleware

object AuthMiddleware {

  private def middleware
    (
      // authChecker: AuthChecker
    )
    : HttpApp[IO] => HttpApp[IO] = {
    inputApp =>
      inputApp

      //   HttpApp[IO] { request =>
      //     val maybeKey = request.headers
      //     .get[`Authorization`]
      //     .collect {
      //       case Authorization(
      //             Credentials.Token(AuthScheme.Bearer, value)
      //           ) =>
      //         value
      //     }
      //     .map { ApiToken.apply }

      //   val isAuthorized = maybeKey
      //     .map { key =>
      //       authChecker.isAuthorized(key)
      //     }
      //     .getOrElse(IO.pure(false))

      //   isAuthorized.ifM(
      //     ifTrue = inputApp(request),
      //     ifFalse = IO.raiseError(new NotAuthorizedError("Not authorized!"))
      //   )
      // }

  }

  def apply
    (
      // authChecker: AuthChecker
    )
    : ServerEndpointMiddleware[IO] =
    new ServerEndpointMiddleware.Simple[IO] {
      private val mid: HttpApp[IO] => HttpApp[IO] = middleware()
      def prepareWithHints
        (
          serviceHints: Hints,
          endpointHints: Hints)
        : HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            endpointHints.get[utils.AuthToken] match {
              case _ => mid
            }
          case None    => identity
        }
      }
    }

}

object Converter:

    val toIO: PolyFunction[Result, IO] =
      new PolyFunction[Result, IO] {

        def apply[A](result: Result[A]): IO[A] = {
          result.foldF(
            error =>
              IO.raiseError(
                error match {
                  case e: ServiceUnavailable => ServiceUnavailableError(e.code, e.title, e.message)
                  case e: Conflict           => ConflictError(e.code, e.title, e.message)
                  case e: BadRequest         => BadRequestError(e.code, e.title, e.message)
                  case e: NotFound           => NotFoundError(e.code, e.title, e.message)
                  case e: InternalServer     => InternalServerError(e.code, e.title, e.message)
                  case e: Unauthorized       => UnauthorizedError(e.code, e.title, e.message)
                  case e: Forbidden          => ForbiddenError(e.code, e.title, e.message)
                }
              ),
            value => IO { value }
          )
        }

      }

trait Service:
    def helloRPC(name: String, town: Option[String]): Future[Greeting]

class ServiceImpl
    (
      using ec: ExecutionContextExecutor) extends Service:

    // given ec: ExecutionContextExecutor = sys.executionContext

    def helloRPC(name: String, town: Option[String]): Future[Greeting] = Future {
      town match {
        case None    => Greeting(s"Hello " + name + "!")
        case Some(t) => Greeting(s"Hello " + name + " from " + t + "!, oiste???")
      }
    }

import ErrorsBuilder.*

class HelloWorldImpl[F[_]]
  (
    service: Service,
    ser: HWService[F]) extends HelloWorldService[F] {

  // def hello(name: String, town: Option[String]): IO[Greeting] = IO.fromFuture(IO(service.helloRPC(name, town)))

  def hello(name: String, town: Option[String]): F[Greeting] = ser.hello(name, town)
  def healthCheck(): F[Unit] = ser.healthCheck()

}

class SmithyResource(service: Service) {

  private def translateMessage(message: String): String = {
    val i = message.indexOf(", offset:")
    if (i == -1)
      message
    else
      message.substring(0, i)
  }

  private val example: Resource[IO, HttpRoutes[IO]] =
      val s = new HWServiceImpl[Result]
      SimpleRestJsonBuilder.routes(
        new HelloWorldImpl[Result](service, s)
          .transform(
            Converter.toIO
          )
      )
        .mapErrors {
          case PayloadError(_, expected, message) =>
            // logger.foreach(_.error(s"${message}"))
            val e = ErrorsBuilder.badRequestError(s"Related to $expected, comment: ${translateMessage(message)}")
            BadRequestError(e.code, e.title, e.message)

          case err: Throwable =>
            val e = internalServerError(err.getMessage())
            InternalServerError(e.code, e.title, e.message)

        }
        .resource

  val all: Resource[IO, HttpRoutes[IO]] = example // .map(_ <+> docs)
}
