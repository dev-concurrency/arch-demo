package event_sourcing
package examples

object WalletEventSourcing:

    import akka.persistence.typed.PersistenceId
    import akka.management.scaladsl.AkkaManagement
    import akka.actor.typed.ActorRef
    import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

    import akka.cluster.typed.*
    import akka.actor.ActorSystem as UntypedActorSystem
    import akka.cluster.ClusterEvent.*

    import com.typesafe.config.Config

    import akka.cluster.sharding.typed.scaladsl.Entity

    import infrastructure.persistence.WalletEntity

    import infrastructure.util.*
    // import infrastructure.util.EitherT_TypesConversion.*

    import akka.event.Logging

    import com.google.rpc.Code

    // composition
    export WalletServices.*
    export WalletServicesImpl.*
    export ClusterInfrastructure.*

    import com.example.*

    import cats.*
    import cats.effect.*
    import cats.implicits.*

    import cats.mtl.*

    import doobie.hikari.HikariTransactor

    object Root:
        trait Command
        object Start           extends Command
        // object StartProjections extends Command
        object StartGrpcServer extends Command
        object StopGrpcServer  extends Command

        var grpcServerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None
        var httpServerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None
        var kafkaConsumerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None

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

                    val transactorResource: Resource[IO, HikariTransactor[IO]] = HikariTransactor.newHikariTransactor[IO](
                      "org.postgresql.Driver",
                      "jdbc:postgresql://localhost:5432/service",
                      "duser",
                      "dpass",
                      ec
                    )

                    import org.typelevel.log4cats.slf4j.Slf4jLogger
                    import org.typelevel.log4cats.Logger
                    import fs2.kafka.*

                    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
                    val cDeser = new CustomDeserializer
                    val consumerData = ConsumerImpl2(cDeser.deserializer)

                    val grpcIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                      shutdown =>
                          grpcServerControl = Some(shutdown)

                          import akka.grpc.GrpcServiceException
                          import com.wallet.demo.clustering.grpc.admin.BadRequestError

                          import fs2.concurrent.Channel

                          given generator: ExceptionGenerator[GrpcServiceException] with
                              def generateException(msg: String): Throwable =
                                  val e = com.example.ErrorsBuilder.badRequestError(msg)
                                  val error = BadRequestError(e.code, e.title, e.message)
                                  GrpcServiceException(Code.INVALID_ARGUMENT, msg, Seq(error))

                          val cSer = new CustomSerializer

                          val resource: Resource[IO, (io.grpc.Server, ProducerImpl)] =
                            for {
                              tx <- transactorResource

                              queue <-
                                Resource.make(Channel.unbounded[IO, ProducerParams])(
                                  ch => IO(ch.close)
                                )
                              ioQueue = new WalletQueueIOImpl(queue)
                              resultQueue = new WalletQueueImpl[GrpcServiceException](ioQueue, MyTransformers[GrpcServiceException])

                              wServiceIO = WalletEventSourcing.WalletServiceIOImpl[Result](ws, resultQueue)
                              producer = ProducerImpl(queue, cSer.serializer)

                              repo = WalletRepositoryIOImpl(tx)
                              wRepo = WalletRepositoryImpl[GrpcServiceException](repo, MyTransformers[GrpcServiceException])
                              serverDefinition <- grpcApi.helloService[GrpcServiceException](wServiceIO, wRepo)
                              server <- grpcApi.run[IO](serverDefinition)

                            } yield (server, producer)
                          // val resource2: Resource[IO, KafkaConsumer[IO, String, org.apache.avro.specific.SpecificRecord] =

                          val x =
                            resource.evalMap(
                              res => {
                                (res._2.init(), IO.pure(res._1.start())).mapN(
                                  (a, c) => ()
                                )
                              }
                            ).useForever

                          IO.race(shutdown.get, x)
                    }

                    val kConsumerIO = KafkaConsumer.resource(consumerData.consumerSettings).use {
                      consumer =>

                          val result: IO[Unit] = IO.uncancelable {
                            poll =>
                              for {
                                runFiber <- consumerData.run(consumer).start
                                _ <- poll(runFiber.join).onCancel {
                                       for {
                                         _ <- IO(println("Starting Kafka consumer graceful shutdown"))
                                         _ <- consumer.stopConsuming
                                         shutdownOutcome <- runFiber
                                                              .join
                                                              .timeoutTo[Outcome[IO, Throwable, Unit]](
                                                                20.seconds,
                                                                IO.pure(
                                                                  Outcome.Errored(
                                                                    new RuntimeException("Graceful shutdown for Kafka consumer timed out")
                                                                  )
                                                                )
                                                              )
                                         _ <-
                                           shutdownOutcome match {
                                             case Outcome.Succeeded(_) => IO(println("Succeeded in Kafka consumer graceful shutdown"))
                                             case Outcome.Canceled()   => IO(println("Canceled in Kafka consumer graceful shutdown")) >> runFiber.cancel
                                             case Outcome.Errored(e)   =>
                                               IO(println("Failed to shutdown gracefully the Kafka consumer ")) >> runFiber.cancel >> IO
                                                 .raiseError(e)
                                           }
                                       } yield ()
                                     }
                              } yield ()
                          }
                          result
                    }

                    val kafkaConsumerIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                      shutdown =>
                          kafkaConsumerControl = Some(shutdown)
                          cats.effect.IO.race(shutdown.get, kConsumerIO)
                    }

                    val httpApi: HttpServerResource = HttpServerResource()
                    val httpIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                      shutdown =>
                          httpServerControl = Some(shutdown)
                          val x = httpApi.helloService.useForever
                          cats.effect.IO.race(shutdown.get, x)
                    }

                    // val consumer = ConsumerImpl(cDeser.deserializer)
                    // Future { consumer.init().logError(er => {
                    //                                     er.printStackTrace()
                    //                                     "Error"
                    //                                   }).evalOn(ctx.system.executionContext).unsafeRunSync() }

                    Future { kafkaConsumerIO.evalOn(ctx.system.executionContext).unsafeRunSync() }

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
                    kafkaConsumerControl.foreach(
                      ser => {
                        Future {
                          val r = Try { ser.complete(true).unsafeRunSync() }
                          println(s"Kafka Consumer Control completed: $r")
                        }
                      }
                    ) // shutdown the server
                    kafkaConsumerControl = None
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
                val adapter = new infrastructure.persistence.AccountDetachedModelsAdapter()

                walletSharding.init(
                  Entity(WalletEntity.typeKey)(createBehavior =
                    entityContext =>
                      WalletEntity(
                        PersistenceId(
                          WalletEntity.typeKey.name,
                          entityContext.entityId,
                        ),
                        adapter
                      )
                  )
                )

                new WalletProjection()
                // wProjection.init()

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
