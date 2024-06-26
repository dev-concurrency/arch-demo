package event_sourcing
package examples

import scala.collection.immutable
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.r2dbc.scaladsl.R2dbcHandler
import akka.projection.r2dbc.scaladsl.R2dbcSession
import infrastructure.persistence.WalletEntity
import infrastructure.persistence.WalletEvents as EventsDataModel
import io.r2dbc.spi.Row
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WalletUPSERTProjectionHandler()(using ec: ExecutionContext)
    extends R2dbcHandler[immutable.Seq[EventEnvelope[EventsDataModel.Event]]]:
    private val logger: Logger = LoggerFactory.getLogger(getClass)

    override def process
        (
          session: R2dbcSession,
          envelopes: immutable.Seq[EventEnvelope[EventsDataModel.Event]])
        : Future[Done] =

        val eventsIds =
          envelopes.map(
            e => e.persistenceId.substring(WalletEntity.typeKey.name.length + 1)
          ).distinct.toList

        val eventsIdsSegment = eventsIds.indices.map(_ + 1).map(
          "$" + _
        ).mkString("(", ", ", ")")

        val finalEventsIdsSegment =
          eventsIdsSegment match
            case "()"  => "('')"
            case other => other

        val stmtRange = session
          .createStatement("""
                          SELECT id, balance
                          FROM wallet WHERE id IN """ + finalEventsIdsSegment)
        val stmtForAllExistingWallets =
          eventsIds
            .zipWithIndex
            .foldLeft(stmtRange):
                case (stmt, (id, i)) => stmt.bind(i, id)

        val existingWalletsTuplesF: Future[IndexedSeq[(String, java.lang.Long)]] =
          session
            .select(stmtForAllExistingWallets)(
              (row: Row) =>
                (row.get("id", classOf[String]), row.get("balance", classOf[java.lang.Long]))
            )

        val batch = existingWalletsTuplesF.map {
          case existingWalletsTuples =>
            val existingWalletsSeq: IndexedSeq[(String, WalletEntity.State)] = existingWalletsTuples.map {
              case (id: String, balance: java.lang.Long) => (id, WalletEntity.State(balance = balance))
            }

            val events = mutable.ArrayBuffer.empty[(String, EventsDataModel.Event)]
            events ++= envelopes.map(
              env => {
                val uuidKey = env.persistenceId.substring(WalletEntity.typeKey.name.length() + 1)
                (uuidKey, env.event)
              }
            )

            val existingWallets: mutable.Map[String, WalletEntity.State] = mutable.Map(existingWalletsSeq*)
            val newItems = mutable.Map.empty[String, WalletEntity.State]
            val walletsToUpdate = mutable.Set.empty[String]

            type UpdateEvents = EventsDataModel.CreditAdded | EventsDataModel.DebitAdded
            while events.nonEmpty do
                val (uuidKey, event) = events.remove(0)
                event match
                  case ev: EventsDataModel.WalletCreated => newItems += (uuidKey -> WalletEntity.onFirstEvent(ev))

                  case ev: UpdateEvents =>
                    if newItems.contains(uuidKey) then
                        newItems.update(uuidKey, newItems(uuidKey).applyEvent(ev))
                    else
                        existingWallets.update(uuidKey, existingWallets(uuidKey).applyEvent(ev))
                        walletsToUpdate += uuidKey

            val inserts = newItems.map:
                case (uuidKey, state) =>
                  logger.info(s"Creating wallet projection for $uuidKey")

                  val optionalFields = List[(String, Option[Any])](
                    ("id", Option(uuidKey)),
                    ("balance", Option(state.balance)),
                  )

                  val fields = optionalFields.filter(_._2.isDefined)
                  val expr = fields
                    .map(_._1)
                    .mkString(", ")

                  val stmtRange = session.createStatement(
                    "INSERT INTO wallet(" + expr + ") VALUES (" + fields.indices.map(_ + 1).map(
                      "$" + _
                    ).mkString(", ") + ")"
                  )

                  fields.map(_._2)
                    .zipWithIndex
                    .foldLeft(stmtRange):
                        case (stmt, (data, i)) => stmt.bind(i, data.get)

            val updates = walletsToUpdate.map:
                case uuidKey =>
                  logger.info(s"Updating wallet projection for $uuidKey")
                  val state = existingWallets(uuidKey)

                  val optionalFields = List[(String, Option[Any])](
                    ("balance", Option(state.balance)),
                  )

                  val fields = optionalFields.filter(_._2.isDefined)
                  val expr = fields
                    .map(_._1)
                    .zipWithIndex
                    .map:
                        case (name, index) => s"$name = $$${index + 1}"
                    .mkString(", ")
                  val stmtRange = session
                    .createStatement("UPDATE wallet SET " + expr + " WHERE id = $" + (fields.size + 1))
                  (fields.map(_._2) ++ List[Option[Any]](Some(uuidKey)))
                    .zipWithIndex
                    .foldLeft(stmtRange):
                        case (stmt, (data, i)) => stmt.bind(i, data.get)

            inserts ++ updates

        }

        batch.map {
          case stmts => session.update(stmts.toVector)
        }.map(
          _ => Done
        )
    end process
