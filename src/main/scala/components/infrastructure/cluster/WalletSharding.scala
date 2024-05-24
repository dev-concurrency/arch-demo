package components
package infrastructure
package cluster

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

class WalletSharding(using sys: ActorSystem[Nothing]):
    val sharding: ClusterSharding = ClusterSharding(sys)
    export sharding.*
