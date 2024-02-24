package demo

trait CborSerializable
trait JsonSerializable

import com.typesafe.config.ConfigFactory

given timeout: Timeout = 3.seconds
val config = ConfigFactory.load(System.getenv("APP_CONFIG_FILE"))

given sys: ActorSystem[Root.Command] = ActorSystem(Root(), "system", config)

given ec: scala.concurrent.ExecutionContext = sys.executionContext

import akka.http.scaladsl.Http.ServerBinding

var binding: Option[ServerBinding] = None
var wallet: Option[ActorRef[Wallet.Command]] = None
