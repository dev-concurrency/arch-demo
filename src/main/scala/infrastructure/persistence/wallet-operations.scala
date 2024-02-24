package event_sourcing
package examples

import akka.actor.typed.delivery.ConsumerController.Start

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

    import infrastructure.persistence.util.*

    import infrastructure.persistence.WalletDataModel.*

    trait WalletService:
        def createWallet(id: String): Future[Done | ResultError]
        def addCredit(id: String, value: Credit): Future[Done | ResultError]
        def addDebit(id: String, value: Debit): Future[Done | ResultError]
        def getBalance(id: String): Future[Balance | ResultError]

    class WalletServiceImpl
        (
          entitySharding: WalletSharding)(using sys: ActorSystem[Nothing]) extends WalletService:

        given ec: ExecutionContextExecutor = sys.executionContext
        given timeout: Timeout = demo.timeout

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

        def interactive(config: Config, grpcApi: ServerModule.gRPCApi, ws: WalletEventSourcing.WalletService): Behavior[Command] = Behaviors.setup[
          Command
        ]:
            (ctx: ActorContext[Command]) =>
                Behaviors.receiveMessage[Command] {
                  case Start           =>
                    // println("Handler started")
                    Behaviors.same
                  case StartGrpcServer =>
                    println("Starting Grpc Server")
                    grpcApi.init(ws)(using ctx.system)
                    Behaviors.same
                  case StopGrpcServer  =>
                    println("Stoping Grpc Server")
                    grpcApi.stop
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

                val grpcApi: ServerModule.gRPCApi = ServerModule.gRPCApi()
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
        case Some(sys) => sys ! Root.StopGrpcServer
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
        sys1.foreach(
          aSys => {
            aSys.terminate()
            given ec: ExecutionContextExecutor = aSys.executionContext
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
