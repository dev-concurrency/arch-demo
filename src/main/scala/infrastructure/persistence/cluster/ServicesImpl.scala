package event_sourcing
package examples

import akka.Done
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.persistence.cassandra.cleanup.Cleanup
import akka.stream.scaladsl.{ Balance as _, * }
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.mtl.*
import com.example.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import fs2.concurrent.Channel
import infrastructure.persistence.*
import infrastructure.persistence.OkResponse
import infrastructure.persistence.WalletDataModel.*
import infrastructure.persistence.WalletDataModel2
import infrastructure.persistence.WalletEntity
import infrastructure.util.*
import io.scalaland.chimney.*
import io.scalaland.chimney.dsl.*

object WalletServicesImpl:
    import WalletServices.*

    class WalletSharding(using sys: ActorSystem[Nothing]):
        val sharding: ClusterSharding = ClusterSharding(sys)
        export sharding.*

    class WalletQueueIOImpl(queue: Channel[IO, ProducerParams]) extends WalletQueueIO[IO]:
        def trySend(data: ProducerParams): IO[Either[Channel.Closed, Boolean]] = queue.trySend(data)

    class WalletQueueImpl[G: ExceptionGenerator](queue: WalletQueueIO[IO], transformers: MyTransformers[G])
        extends WalletQueue[Result]:
        import transformers.queueToResultTransformer

        def trySend(data: ProducerParams): Result[Boolean] = {
          val r = queue.trySend(data)
          r.transformInto[Result[Boolean]]
        }

    class WalletRepositoryImpl[G: ExceptionGenerator](wRepo: WalletRepositoryIO[IO], transformers: MyTransformers[G])
        extends WalletRepository[Result]:
        import transformers.*
        def deleteWallet(id: String): Result[Int] = wRepo.deleteWallet(id).transformInto[Result[Int]]

    class WalletRepositoryIOImpl(tx: HikariTransactor[IO]) extends WalletRepositoryIO[IO]:
        def deleteWallet(id: String): IO[Either[Throwable, Int]] = sql"DELETE FROM wallet WHERE id = $id".update.run.transact(tx).attempt

    def reportError[F[_], A](code: TransportError, message: String)(using FR: Raise[F, ServiceError]): F[A] =
      code match {
        case TransportError.NotFound => FR.raise(ErrorsBuilder.notFoundError(message))
        case _                       => FR.raise(ErrorsBuilder.internalServerError(message))
      }

    class WalletServiceIO_2Impl[F[_]](entitySharding: WalletSharding)
        (using sys: ActorSystem[Nothing], F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F]) extends WalletServiceIO_2[F]:

        given ec: ExecutionContextExecutor = sys.executionContext
        given timeout: Timeout = demo.timeout

        def createWallet(id: String): F[Done] =
          for {
            res <- F.fromFuture(entitySharding
                     .entityRefFor(WalletEntity.typeKey, id)
                     .ask(WalletEntity.CreateWalletCmd(_))
                     .mapTo[Done | ResultError].pure[F])
            done <-
              res match {
                case Done                       => F.pure(Done)
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def deleteWallet(id: String): F[Done] =
          for {
            res <- F.fromFuture(
                     entitySharding
                       .entityRefFor(WalletEntity.typeKey, id)
                       .ask(WalletEntity.StopCmd(_))
                       .mapTo[Done | ResultError].pure[F]
                   )
            done <-
              res match {
                case Done                       =>
                  val persistenceIdParallelism = 10
                  // val queries = PersistenceQuery(sys).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
                  // queries.currentPersistenceIds().mapAsync(persistenceIdParallelism)(pid =>
                  //   println(s"pid: $pid")
                  //   Future(())
                  //   ).run()
                  val cleanup = new Cleanup(sys)
                  val res = Source.single(s"${WalletEntity.typeKey.name}|$id")
                    .mapAsync(persistenceIdParallelism)(
                      i => {
                        println(s"Deleting: $i")
                        // val rr = cleanup.deleteAllEvents(i, false)
                        val rr = cleanup.deleteAll(i, false)
                        rr.onComplete {
                          case Failure(exception) => println(s"Error 1: $exception")
                          case Success(value)     => println(s"Deleted 1: $value")
                        }
                        rr
                      }
                    )
                    .runWith(Sink.ignore)
                  F.fromFuture(res.pure[F])
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

    class WalletServiceIOImpl[F[_]]
        (
          wService: WalletService,
          queue: WalletQueue[F])(using ec: ExecutionContextExecutor, F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
        extends WalletServiceIO[F]:

        def deleteWallet(id: String): F[Done] =
          for {
            res <- F.fromFuture(wService.deleteWallet(id).pure[F])
            done <-
              res match {
                case Done                       => F.pure(Done)
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def createWallet(id: String): F[Done] =
          for {
            res <- F.fromFuture(wService.createWallet(id).pure[F])
            done <-
              res match {
                case Done                       => F.pure(Done)
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def addCredit(id: String, value: Credit): F[Done] =
            val record = org.integration.avro.transactions.CreditRequest.newBuilder()
              .setId(id)
              .setAmount(value.amount)
              .build()
            for {
              res <- F.fromFuture(wService.addCredit(id, value).pure[F])
              _ <- queue.trySend(ProducerParams("topic", id, record, Map()))
              done <-
                res match {
                  case Done                       => F.pure(Done)
                  case ResultError(code, message) => reportError(code, message)
                }
            } yield done

        def addDebit(id: String, value: Debit): F[Done] =
          for {
            res <- F.fromFuture(wService.addDebit(id, value).pure[F])
            done <-
              res match {
                case Done                       => F.pure(Done)
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def getBalance(id: String): F[Balance] =
          for {
            res <- F.fromFuture(wService.getBalance(id).pure[F])
            balance <-
              res match {
                case b: Balance                 => F.pure(b)
                case ResultError(code, message) => reportError(code, message)
              }
          } yield balance

    class WalletServiceIOImpl2[F[_]]
        (
          wService: WalletService2,
          queue: WalletQueue[F])(using ec: ExecutionContextExecutor, F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
        extends WalletServiceIO2[F]:

        def deleteWallet(id: String): F[OkResponse] =
          for {
            res <- F.fromFuture(wService.deleteWallet(id).pure[F])
            done <-
              res match {
                case OkResponse()               => F.pure(OkResponse())
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def createWallet(id: String): F[OkResponse] =
          for {
            res <- F.fromFuture(wService.createWallet(id).pure[F])
            done <-
              res match {
                case OkResponse()               => F.pure(OkResponse())
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def addCredit(id: String, value: WalletDataModel2.Credit): F[OkResponse] =
            val record = org.integration.avro.transactions.CreditRequest.newBuilder()
              .setId(id)
              .setAmount(value.amount)
              .build()
            for {
              res <- F.fromFuture(wService.addCredit(id, value).pure[F])
              _ <- queue.trySend(ProducerParams("topic", id, record, Map()))
              done <-
                res match {
                  case OkResponse()               => F.pure(OkResponse())
                  case ResultError(code, message) => reportError(code, message)
                }
            } yield done

        def addDebit(id: String, value: WalletDataModel2.Debit): F[OkResponse] =
          for {
            res <- F.fromFuture(wService.addDebit(id, value).pure[F])
            done <-
              res match {
                case OkResponse()               => F.pure(OkResponse())
                case ResultError(code, message) => reportError(code, message)
              }
          } yield done

        def getBalance(id: String): F[WalletDataModel2.Balance] =
          for {
            res <- F.fromFuture(wService.getBalance(id).pure[F])
            balance <-
              res match {
                case b: WalletDataModel2.Balance => F.pure(b)
                case ResultError(code, message)  => reportError(code, message)
              }
          } yield balance

    class WalletServiceImpl
        (
          entitySharding: WalletSharding)(using sys: ActorSystem[Nothing]) extends WalletService:

        given ec: ExecutionContextExecutor = sys.executionContext
        given timeout: Timeout = demo.timeout

        def deleteWallet(id: String): Future[Done | ResultError] = entitySharding
          .entityRefFor(WalletEntity.typeKey, id)
          .ask(WalletEntity.StopCmd(_))
          .mapTo[Done | ResultError].map:
              case d: Done        =>
                val persistenceIdParallelism = 10
                // val queries = PersistenceQuery(sys).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
                // queries.currentPersistenceIds().mapAsync(persistenceIdParallelism)(pid =>
                //   println(s"pid: $pid")
                //   Future(())
                //   ).run()
                val cleanup = new Cleanup(sys)
                Source.single(s"${WalletEntity.typeKey.name}|$id")
                  .mapAsync(persistenceIdParallelism)(
                    i => {
                      println(s"Deleting: $i")
                      // val rr = cleanup.deleteAllEvents(i, false)
                      val rr = cleanup.deleteAll(i, false)
                      rr.onComplete {
                        case Failure(exception) => println(s"Error 1: $exception")
                        case Success(value)     => println(s"Deleted 1: $value")
                      }
                      rr
                    }
                  )
                  .runWith(Sink.ignore)
                d
              case e: ResultError => e

        def createWallet(id: String): Future[Done | ResultError] = entitySharding
          .entityRefFor(WalletEntity.typeKey, id)
          .ask(WalletEntity.CreateWalletCmd(_))
          .mapTo[Done | ResultError]

        def addCredit(id: String, value: Credit): Future[Done | ResultError] = entitySharding
          .entityRefFor(WalletEntity.typeKey, id)
          .ask(WalletEntity.CreditCmd(value.amount, _))
          .mapTo[Done | ResultError]

        def addDebit(id: String, value: Debit): Future[Done | ResultError] = entitySharding
          .entityRefFor(WalletEntity.typeKey, id)
          .ask(WalletEntity.DebitCmd(value.amount, _))
          .mapTo[Done | ResultError]

        def getBalance(id: String): Future[Balance | ResultError] = entitySharding
          .entityRefFor(WalletEntity.typeKey, id)
          .ask(WalletEntity.GetBalanceCmd(_))
          .mapTo[Balance | ResultError]

    class WalletServiceImpl2
        (
          entitySharding: WalletSharding)(using sys: ActorSystem[Nothing]) extends WalletService2:

        // import infrastructure.persistence.WalletCommands.*
        import infrastructure.persistence.WalletCommands2.CommandsADT
        import infrastructure.persistence.WalletCommands2
        import infrastructure.persistence.WalletEntity2

        given ec: ExecutionContextExecutor = sys.executionContext
        given timeout: Timeout = demo.timeout

        def deleteWallet(id: String): Future[OkResponse | ResultError] = ???

        def createWallet(id: String): Future[OkResponse | ResultError] = entitySharding
          .entityRefFor(WalletEntity2.typeKey, id)
          .ask(
            // WalletCommands2.CmdInst[CommandsADT, Done](CommandsADT.CreateWalletCmd, List(id), _)
            FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _)
          )
          .mapTo[OkResponse | ResultError]

        def addCredit(id: String, value: WalletDataModel2.Credit): Future[OkResponse | ResultError] = entitySharding
          .entityRefFor(WalletEntity2.typeKey, id)
          .ask(
            // WalletCommands2.CmdInst[CommandsADT, Done](CommandsADT.CreditCmd(value), List(id), _)
            FrameWorkCommands.CmdInst(CommandsADT.CreditCmd(value), List(id), _)
          )
          .mapTo[OkResponse | ResultError]

        def addDebit(id: String, value: WalletDataModel2.Debit): Future[OkResponse | ResultError] = ???
        // entitySharding
        // .entityRefFor(WalletEntity2.typeKey, id)
        // .ask(
        //   WalletCommands2.CmdInst[CommandsADT, Done](CommandsADT.DebitCmd(value.amount), List("id" -> id), _)
        //   )
        // .mapTo[Done | ResultError]

        def getBalance(id: String): Future[WalletDataModel2.Balance | ResultError] = entitySharding
          .entityRefFor(WalletEntity2.typeKey, id)
          .ask(
            // WalletCommands2.CmdInst[CommandsADT, WalletDataModel2.Balance](CommandsADT.GetBalanceCmd, List(id), _)
            FrameWorkCommands.CmdInst(CommandsADT.GetBalanceCmd, List(id), _)
            // WalletCommands2.CmdInst(WalletCommands2.Payload(WalletCommands2.GetBalanceCmd()), List("id" -> id), _)
          )
          .mapTo[WalletDataModel2.Balance | ResultError]
