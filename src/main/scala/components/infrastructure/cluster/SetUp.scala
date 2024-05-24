package components
package infrastructure
package cluster

import akka.actor.typed.ActorRef
import akka.cluster.ClusterEvent.*
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.cluster.typed.*
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.Config

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.persistence.typed.PersistenceId

import org.slf4j.{ Logger, LoggerFactory }
import akka.cluster.pubsub.protobuf.msg.DistributedPubSubMessages.Send

import com.typesafe.config.ConfigFactory
import distage.ModuleDef
import distage.Injector
import distage.Roots
import distage.DIKey
import distage.plugins.PluginConfig
import distage.plugins.PluginLoader

trait Registration:
    def register(ctx: ActorContext[Root.Command]): Unit

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

import infrastructure.wallet.WalletContainer as obj

object Root:
    trait Command
    object Start           extends Command
    // object StartProjections extends Command
    object StartGrpcServer extends Command
    object StopGrpcServer  extends Command
    object SendMsg         extends Command
    object CreateMsg       extends Command
    object GetMsg          extends Command

    def interactive
      (
        ws: WalletSharding,
        WEntity: obj.EntityConfig,
        ): Behavior[Command] = Behaviors.setup[ Command ]:
        (ctx: ActorContext[Command]) =>

            given ec: ExecutionContextExecutor = ctx.system.executionContext

            import akka.event.Logging
            import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
            val log = Logging(ctx.system.toClassic, classOf[Command])

            import components.infrastructure.wallet.Commands
            import components.infrastructure.wallet.DataModel
            import _root_.infrastructure.util.*
            import akka.Done
            
            Behaviors.receiveMessage[Command] {
              case Start   =>
                // println("Handler started")
                Behaviors.same

              case CreateMsg =>
                println("Sending CreateMsg")
                
                given timeout: Timeout = demo.timeout
                
                val res = ws
                .entityRefFor(WEntity.typeKey, "a")
                .ask(Commands.CreateWalletCmd(_))
                .mapTo[Done | ResultError]
                res.onComplete {
                  case Success(value)     => println(s"Got the callback, value = $value")
                  case Failure(exception) => println(s"Got the callback, exception = $exception")
                }
                Behaviors.same

              case SendMsg =>
                println("Sending msg")
                
                given timeout: Timeout = demo.timeout
                
                val res = ws
                .entityRefFor(WEntity.typeKey, "a")
                .ask(Commands.CreditCmd(20, _))
                .mapTo[Done | ResultError]
                res.onComplete {
                  case Success(value)     => println(s"Got the callback, value = $value")
                  case Failure(exception) => println(s"Got the callback, exception = $exception")
                }
                Behaviors.same

              case GetMsg =>
                println("Sending GetMsg")
                
                given timeout: Timeout = demo.timeout
                
                val res = ws
                .entityRefFor(WEntity.typeKey, "a")
                .ask(Commands.GetBalanceCmd(_))
                .mapTo[DataModel.Balance | ResultError]
                res.onComplete {
                  case Success(value)     => println(s"Got the callback, value = $value")
                  case Failure(exception) => println(s"Got the callback, exception = $exception")
                }
                Behaviors.same
            }

    // def apply
    //   (
    //     registriation: Registration,
    //     we: WalletEntitySetup,
    //     WEntity: obj.EntityConfig)
    //   : Behavior[Command] = Behaviors.setup[Command]:
    //     (ctx: ActorContext[Command]) =>
    //         ctx.log.info("Starting Wallet Operations")
    //         registriation.register(ctx)
    //         val ws = we.setup()
    //         ctx.delegate(interactive(ws, WEntity), Root.Start)

    def apply(conf: Config) : Behavior[Command] = Behaviors.setup[Command]:
        (ctx: ActorContext[Command]) =>
            ctx.log.info("Starting Wallet Operations")
            
            def ctxModule = new ModuleDef{
              make[Config].from(conf)
              make[ActorSystem[?]].from(ctx.system)
              make[WalletSharding]
              make[WalletEntitySetup].fromTrait[WalletEntitySetupImpl]
              make[Registration].fromTrait[RegistrationImpl]
              make[obj.EntityConfig].from{
                // println("Creating EntityConfig ==================================")
                val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("components.entities"))
                val appModules = PluginLoader().load(pluginConfig)
                val module = appModules.result.merge

                val entity = Injector().produceGet[obj.EntityConfig](module).unsafeGet()
                entity
              }
              
              make[ActorSystem[?]].from(ctx.system)
              make[ActorSystem[Nothing]].from(ctx.system)
              make[ActorSystem[Root.Command]].from(
                ctx.system.asInstanceOf[ActorSystem[Root.Command]]
              )
              make[ExecutionContextExecutor].from(ctx.system.executionContext)
            }

            import scala.util.*
        
            val res: Try[Behavior[Command]] = Try{Injector().produceRun(ctxModule) {
              (
                // conf: Config,
                // root: Behavior[Root.Command],
                
                WEntity: obj.EntityConfig,
                wes: WalletEntitySetup,
                register: Registration,

                sharding: WalletSharding,
                sys: ActorSystem[Root.Command],

              ) => {
                  register.register(ctx)
                  val ws = wes.setup()
                  ctx.delegate(interactive(ws, WEntity), Root.Start)
                }
            }
            }
            res match{
              case Success(value) => value
              case Failure(exception) => 
                exception.printStackTrace()
                Behaviors.empty
            }

import Root.*

trait RegistrationImpl
    (
      config: Config) extends Registration:

    // println("Creating RegistrationImpl ==================================")
    def register(ctx: ActorContext[Command]): Unit =

        given typedActorSystem: ActorSystem[Nothing] = ctx.system
        given ExecutionContextExecutor = ctx.system.executionContext

        _root_.infrastructure.Serializers.register(typedActorSystem)

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

trait WalletEntitySetup:
    def setup(): WalletSharding

trait WalletEntitySetupImpl
    (
      // sys: ActorSystem[Root.Command],
      sharding: WalletSharding,
      entity: obj.EntityConfig) extends WalletEntitySetup:

    // println("Creating WalletEntitySetupImpl ==================================")
    
    // given typedActorSystem: ActorSystem[Nothing] = sys
    def setup(): WalletSharding =
        given logger: Logger = LoggerFactory.getLogger(getClass)
        sharding.init(
          Entity(entity.typeKey)(createBehavior =
            entityContext =>
              entity(
                PersistenceId(
                  entity.typeKey.name,
                  entityContext.entityId,
                )
              )
          )
        )

        sharding


object WalletOperations:

    
    val confFile = "application-clustering.conf"
    val actorSystemName = "system"

    var sys1: Option[ActorSystem[Root.Command]] = None
    var sys2: Option[ActorSystem[Root.Command]] = None
    var sys3: Option[ActorSystem[Root.Command]] = None

    def module =
      new ModuleDef {
      }
        
    def start1 =
        // akka.loglevel = "DEBUG"
        val conf: Config = ConfigFactory.parseString(
          """
             application.local.config.first = true
             akka.remote.artery.canonical.port = 2551
          """
        )
          .withFallback(ConfigFactory.load(confFile))
        val sys = ActorSystem[Root.Command](Root(conf), actorSystemName, conf)
        sys1 = Some(sys)

        
    def start2 =

        val conf: Config = ConfigFactory.parseString(
          s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2552
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
          """
        )
          .withFallback(ConfigFactory.load(confFile))
        val sys = ActorSystem[Root.Command](Root(conf), actorSystemName, conf)
        sys2 = Some(sys)
        
    def start3 =
        val conf: Config = ConfigFactory.parseString(
          s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2553
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
          """
        )
          .withFallback(ConfigFactory.load(confFile))
        val sys = ActorSystem[Root.Command](Root(conf), actorSystemName, conf)
        sys3 = Some(sys)
        
    def init =
        start1
        Thread.sleep(3000)
        start2
        start3
        // Thread.sleep(3000)
        // send
        Thread.sleep(3000)
        get

    def create =
        sys1.foreach(
          aSys => {
            println("Sending CreateMsg")
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys ! CreateMsg
          }
        )
        
    def send =
        sys1.foreach(
          aSys => {
            println("Sending SendMsg")
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys ! SendMsg
          }
        )

    def get =
        sys1.foreach(
          aSys => {
            println("Sending GetMsg")
            given ec: ExecutionContextExecutor = aSys.executionContext
            aSys ! GetMsg
          }
        )

    def stop =

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

// def runDemo =
//     import distage.ModuleDef
//     import distage.Injector
//     import com.typesafe.config.ConfigFactory

//     val confFile = "application-clustering.conf"
//     val actorSystemName = "system"

//     val conf = ConfigFactory.parseString(
//       """
//          application.local.config.first = true
//          akka.remote.artery.canonical.port = 2551
//       """
//     )
//       .withFallback(ConfigFactory.load(confFile))

//     def module =
//       new ModuleDef {
//         make[ActorSystem[Command]].from {
//           (
//             register: Registration,
//             wes: WalletEntitySetup,
//             WEntity: obj.EntityConfig) =>
//             ActorSystem(Root(register, wes, WEntity), actorSystemName, conf)
//         }
//         make[WalletSharding]
//         make[WalletEntitySetup].fromTrait[WalletEntitySetupImpl]
//       }

//     Injector().produceRun(module) {
//       (
//         we: WalletEntitySetup,
//       ) =>
//         {
//           we.setup()
//         }
//     }

// @main
// def run = runDemo
