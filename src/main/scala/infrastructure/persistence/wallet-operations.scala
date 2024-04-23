package event_sourcing
package examples

import akka.actor.typed.delivery.ConsumerController.Start
import cats.effect.IO

object WalletEventSourcing:

    import com.typesafe.config.ConfigFactory
    import akka.persistence.typed.PersistenceId
    import akka.management.cluster.bootstrap.ClusterBootstrap
    import akka.management.scaladsl.AkkaManagement
    import akka.actor.typed.ActorRef
    import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

    import akka.cluster.typed.*
    import akka.actor.ActorSystem as UntypedActorSystem
    import akka.cluster.ClusterEvent.*
    import akka.cluster.Member
    import akka.cluster.MemberStatus

    import com.typesafe.config.Config

    import akka.Done
    import akka.cluster.sharding.typed.scaladsl.ClusterSharding
    import akka.cluster.sharding.typed.scaladsl.Entity

    import infrastructure.persistence.WalletEntity

    import infrastructure.util.*
    // import infrastructure.util.EitherT_TypesConversion.*

    import infrastructure.persistence.WalletDataModel.*

    import java.time.{ LocalDate, ZonedDateTime }
    import java.time.temporal.ChronoUnit

    import akka.persistence.cassandra.cleanup.Cleanup
    import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
    import akka.persistence.query.PersistenceQuery
    import akka.stream.scaladsl.{ Balance => _, * }

    import fs2.grpc.syntax.all.*

    import akka.event.Logging

    import com.google.rpc.Code
    import io.grpc.*

    import com.example.*

    trait WalletServiceIO[F[_]]:
        def createWallet(id: String): F[Done]
        def deleteWallet(id: String): F[Done]
        def addCredit(id: String, value: Credit): F[Done]
        def addDebit(id: String, value: Debit): F[Done]
        def getBalance(id: String): F[Balance]

    trait WalletServiceIO2[F[_]]:
        def createWallet(id: String): F[Done]
        def deleteWallet(id: String): F[Done]

    import cats.*
    import cats.effect.*
    import cats.implicits.*
    import cats.instances.*

    import cats.syntax.all.*

    import cats.mtl.*
    import cats.mtl.implicits.*

    def reportError[F[_], A](code: TransportError, message: String)(using FR: Raise[F, ServiceError]): F[A] =
      code match {
        case TransportError.NotFound => FR.raise(ErrorsBuilder.notFoundError(message))
        case _                       => FR.raise(ErrorsBuilder.internalServerError(message))
      }

    class WalletServiceIO2Impl[F[_]](entitySharding: WalletSharding)
        (using sys: ActorSystem[Nothing], F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F]) extends WalletServiceIO2[F]:

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

    class WalletServiceIOImpl[F[_]](wService: WalletService)
        (using ec: ExecutionContextExecutor, F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F]) extends WalletServiceIO[F]:

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
          for {
            res <- F.fromFuture(wService.addCredit(id, value).pure[F])
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

    trait WalletService:
        def createWallet(id: String): Future[Done | ResultError]
        def deleteWallet(id: String): Future[Done | ResultError]
        def addCredit(id: String, value: Credit): Future[Done | ResultError]
        def addDebit(id: String, value: Debit): Future[Done | ResultError]
        def getBalance(id: String): Future[Balance | ResultError]

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

    class WalletSharding(using sys: ActorSystem[Nothing]):
        val sharding: ClusterSharding = ClusterSharding(sys)
        export sharding.*

    object ClusterStateChanges:

        def apply(): Behavior[MemberEvent] = Behaviors.setup:
            ctx =>
                Behaviors.receiveMessage:
                    case MemberJoined(member: Member) =>
                      ctx.log.info("MemberJoined: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberWeaklyUp(member: Member) =>
                      ctx.log.info("MemberWeaklyUp: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberUp(member: Member) =>
                      ctx.log.info("MemberUp: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberLeft(member: Member) =>
                      ctx.log.info("MemberLeft: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberPreparingForShutdown(member: Member) =>
                      ctx.log.info("MemberPreparingForShutdown: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberReadyForShutdown(member: Member) =>
                      ctx.log.info("MemberReadyForShutdown: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberExited(member: Member) =>
                      ctx.log.info("MemberExited: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberDowned(member: Member) =>
                      ctx.log.info("MemberDowned: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberRemoved(member: Member, previousStatus: MemberStatus) =>
                      ctx.log.info2(
                        "MemberRemoved: {}, previousStatus: {}",
                        member.uniqueAddress,
                        previousStatus
                      )
                      Behaviors.same

    object Root:
        trait Command
        object Start           extends Command
        // object StartProjections extends Command
        object StartGrpcServer extends Command
        object StopGrpcServer  extends Command

        var grpcServerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None
        var httpServerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None

        def interactive
          (
            config: Config,
            grpcApi: GrpcServerResource
            // , grpcApi: ServerModule.gRPCApi
            ,
            ws: WalletEventSourcing.WalletService)
          : Behavior[Command] = Behaviors.setup[
          Command
        ]:
            (ctx: ActorContext[Command]) =>

                given ec: ExecutionContextExecutor = ctx.system.executionContext

                val log = Logging(ctx.system.toClassic, classOf[Command])

                Behaviors.receiveMessage[Command] {
                  case Start           =>
                    println("Handler started")
                    Behaviors.same
                  case StartGrpcServer =>
                    println("Starting Grpc Server")
                    log.info("Starting Grpc Server in logs")
                    ctx.log.info("Starting Grpc Server in ctx")
                    // grpcApi.init(ws)(using ctx.system)
                    import cats.effect.unsafe.implicits.global
                    // val myIO: IO[Nothing] = grpcApi.gRPCServer[cats.effect.IO]

                    val grpcIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                      shutdown =>
                          grpcServerControl = Some(shutdown)
                          import akka.grpc.GrpcServiceException

                          given generator: ExceptionGenerator[GrpcServiceException] with
                              def generateException(msg: String): Throwable =
                                  val error = com.wallet.demo.clustering.grpc.admin.BadRequestError("EE", "Code", msg)
                                  GrpcServiceException(Code.INVALID_ARGUMENT, msg, Seq(error))

                          val wServiceIO = WalletEventSourcing.WalletServiceIOImpl[Result](ws)
                          val resource =
                            for {
                              serverDefinition <- grpcApi.helloService[GrpcServiceException](wServiceIO)
                              server <- grpcApi.run[cats.effect.IO](serverDefinition)
                            } yield server

                          val x =
                            resource.evalMap(
                              server => cats.effect.IO.pure(server.start())
                            ).useForever
                          cats.effect.IO.race(shutdown.get, x)
                    }
                    val httpApi: HttpServerResource = HttpServerResource()
                    val httpIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                      shutdown =>
                          httpServerControl = Some(shutdown)
                          val x = httpApi.helloService.useForever
                          cats.effect.IO.race(shutdown.get, x)
                    }

                    Future { grpcIO.evalOn(ctx.system.executionContext).unsafeRunSync() }
                    Future { httpIO.evalOn(ctx.system.executionContext).unsafeRunSync() }

                    Behaviors.same

                  case StopGrpcServer =>
                    import cats.effect.unsafe.implicits.global
                    println("Stoping servers")
                    grpcServerControl.foreach(
                      ser => {
                        Future {
                          val r = Try { ser.complete(true).unsafeRunSync() }
                          println(s"Grpc Server Control completed: $r")
                        }
                      }
                    ) // shutdown the server
                    grpcServerControl = None
                    httpServerControl.foreach(
                      ser => {
                        Future {
                          val r = Try { ser.complete(true).unsafeRunSync() }
                          println(s"Http Server Control completed: $r")
                        }
                      }
                    ) // shutdown the server
                    httpServerControl = None
                    Behaviors.same
                }

        def apply(config: Config): Behavior[Command] = Behaviors.setup[Command]:
            (ctx: ActorContext[Command]) =>
                ctx.log.info("Starting Wallet Operations")
                given typedActorSystem: ActorSystem[Nothing] = ctx.system
                given UntypedActorSystem = typedActorSystem.toClassic
                given ExecutionContextExecutor = ctx.system.executionContext

                infrastructure.Serializers.register(typedActorSystem)

                val cluster = Cluster(typedActorSystem)
                ctx.log.info(
                  "Started [" + ctx.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")"
                )

                if config.getBoolean("application.local.config.first") then
                    cluster.manager ! Join(cluster.selfMember.address)
                    val management = AkkaManagement(typedActorSystem).start()
                    management.onComplete:
                        case Failure(exception) => println(s"Akka Management failed to start: $exception")
                        case Success(value)     => println(s"Akka Management started at: $value")

                val subscriber = ctx.spawnAnonymous(ClusterStateChanges())
                cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

                val walletSharding = WalletSharding()

                walletSharding.init(
                  Entity(WalletEntity.typeKey)(createBehavior =
                    entityContext =>
                      WalletEntity(
                        PersistenceId(
                          WalletEntity.typeKey.name,
                          entityContext.entityId,
                        )
                      )
                  )
                )

                val wProjection = new WalletProjection()
                wProjection.init()

                // val grpcApi: ServerModule.gRPCApi = ServerModule.gRPCApi()
                val grpcApi: GrpcServerResource = GrpcServerResource()
                val w: WalletEventSourcing.WalletService = new WalletServiceImpl(walletSharding)
                ctx.delegate(interactive(config, grpcApi, w), Root.Start)

object WalletOperations:

    import WalletEventSourcing.*
    import com.typesafe.config.ConfigFactory

    val confFile = "application-clustering.conf"
    val actorSystemName = "system"

    var sys1: Option[ActorSystem[Root.Command]] = None
    var sys2: Option[ActorSystem[Root.Command]] = None
    var sys3: Option[ActorSystem[Root.Command]] = None

    def start1 =
        // akka.loglevel = "DEBUG"
        val conf = ConfigFactory.parseString(
          """
             application.local.config.first = true
             akka.remote.artery.canonical.port = 2551
          """
        )
          .withFallback(ConfigFactory.load(confFile))
        val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
        sys1 = Some(sys)

    def gRPCServerStart =
      sys1 match
        case Some(sys) => sys ! Root.StartGrpcServer
        case None      => println("Actor system not started")

    def gRPCServerStop =
      sys1 match
        case Some(sys) =>
          println("Sending Root.StopGrpcServer")
          sys ! Root.StopGrpcServer
        case None      => println("Actor system not started")

    def start2 =
        val conf = ConfigFactory.parseString(
          s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2552
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
            """
        )
          .withFallback(ConfigFactory.load(confFile))

        val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
        sys2 = Some(sys)

    def start3 =
        // "akka://${actorSystemName}@0.0.0.0:2552"
        val conf = ConfigFactory.parseString(
          s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2553
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
            """
        )
          .withFallback(ConfigFactory.load(confFile))
        val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
        sys3 = Some(sys)

    def stop =

        gRPCServerStop

        sys1.foreach(
          aSys => {
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys.terminate()
            aSys.whenTerminated.onComplete(
              _ => println("Actor system 1 was stopped")
            )
          }
        )
        sys2.foreach(
          aSys => {
            aSys.terminate()
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys.whenTerminated.onComplete(
              _ => println("Actor system 2 was stopped")
            )
          }
        )
        sys3.foreach(
          aSys => {
            aSys.terminate()
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys.whenTerminated.onComplete(
              _ => println("Actor system 3 was stopped")
            )
          }
        )
        sys1 = None
        sys2 = None
        sys3 = None

    def init =
        start1
        Thread.sleep(3000)
        start2
        start3
        gRPCServerStart
