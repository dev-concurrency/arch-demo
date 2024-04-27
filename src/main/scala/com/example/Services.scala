package com.example

import hello.*

import cats.*
import cats.effect.*


import cats.mtl.*

trait HWService[F[_]]:
    def hello(name: String, town: Option[String]): F[Greeting]
    def healthCheck(): F[Unit]

class HWServiceImpl[F[_]](using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
    extends HWService[F]:

    def ch1(name: String, town: Option[String]): F[Greeting] = F.pure(Greeting(s"Hello $name from $town!"))

    def ch2(): F[Unit] = F.pure(Greeting(s"Hello from ch2!"))

    def hello(name: String, town: Option[String]): F[Greeting] =
        // IO{throw new Exception("xxxxxxxxxxx")}.unsafeRunSync()

        F.pure(Greeting(s"Hello $name from $town!"))
        FR.raise(ErrorsBuilder.badRequestError("bad request"))
        // MT.raiseError(new Exception("error ---------------"))

    def healthCheck(): F[Unit] = F.pure(())
