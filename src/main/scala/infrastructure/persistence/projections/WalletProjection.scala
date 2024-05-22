package event_sourcing
package examples

import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorSystem
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.SingletonActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.r2dbc.scaladsl.R2dbcProjection
import akka.projection.scaladsl.SourceProvider
import infrastructure.persistence.WalletEvents as EventsDataModel

class WalletProjection()(using system: ActorSystem[Nothing]):

    def init(): Unit =
        given ec: ExecutionContext = system.executionContext
        val projectionTag = "wallet"
        val targetTag = "UPSERT"
        val sourceProvider: SourceProvider[Offset, EventEnvelope[EventsDataModel.Event]] = EventSourcedProvider.eventsByTag[
          EventsDataModel.Event
        ](
          system = system,
          readJournalPluginId = CassandraReadJournal.Identifier,
          tag = targetTag
        )
        val projection = R2dbcProjection
          .groupedWithin(
            ProjectionId(projectionTag, targetTag),
            settings = None,
            sourceProvider,
            handler = () => new WalletUPSERTProjectionHandler()
          )
          .withGroup(groupAfterEnvelopes = 2, groupAfterDuration = 3.seconds)
        ClusterSingleton(system).init(
          SingletonActor(
            ProjectionBehavior(projection),
            projection.projectionId.id
          )
            .withStopMessage(ProjectionBehavior.Stop)
        )
