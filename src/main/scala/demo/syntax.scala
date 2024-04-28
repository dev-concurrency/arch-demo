package demo
package logging

import cats.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

// import org.typelevel.log4cats.slf4j.Slf4jLogger
// given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

object syntax:

    extension [F[_], E, A](fa: F[A])(using me: MonadError[F, E], logger: Logger[F])

        def log(success: A => String, error: E => String): F[A] = fa.attemptTap:
            case Right(a) =>
              logger.info(success(a))
            case Left(e)  =>
              logger.error(error(e))

        def logError(error: E => String): F[A] = fa.attemptTap:
            case Right(_) => ().pure[F]
            case Left(e)  =>
              logger.error(error(e))
