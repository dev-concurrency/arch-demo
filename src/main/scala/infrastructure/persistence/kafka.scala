package event_sourcing
package examples

import cats.effect.*
import fs2.*
import fs2.concurrent.*

trait Producer[T]:
    def produce(value: T): IO[Either[Channel.Closed, Unit]]
    def init(): IO[Unit]

import org.apache.avro.specific.SpecificRecord

case class ProducerParams(topic: String, key: String, value: SpecificRecord, headers: Map[String, String])

import fs2.concurrent.Channel

import _root_.io.confluent.kafka.serializers.{
  AbstractKafkaSchemaSerDeConfig,
  KafkaAvroDeserializer,
  KafkaAvroDeserializerConfig,
  KafkaAvroSerializer
}

import org.apache.kafka.common.serialization.Serializer as KSerializer
import org.apache.kafka.common.serialization.Deserializer as KDeserializer

import fs2.kafka.*

import scala.jdk.CollectionConverters.*
import IO.asyncForIO

import java.nio.charset.StandardCharsets

class CustomSerializer {

  private val kafkaAvroSerDeConfig = Map[String, Any](
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> "http://localhost:18081",
    AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> "true",
    AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> "registry.strategy.RecordSubjectStrategy",
    AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION -> false.toString,
  )

  private val kafkaAvroSerializer = new KafkaAvroSerializer()
  kafkaAvroSerializer.configure(kafkaAvroSerDeConfig.asJava, false)

  val serializer = kafkaAvroSerializer.asInstanceOf[KSerializer[SpecificRecord]]
}

class ProducerImpl(queue: Channel[IO, ProducerParams], serializer: KSerializer[SpecificRecord]) extends Producer[ProducerParams]:

    def init(): IO[Unit] =
        val producerSettings = ProducerSettings(
          keySerializer = Serializer[IO, String],
          valueSerializer = Serializer.delegate(serializer),
        )
          .withBootstrapServers("localhost:19092")
        queue
          .stream
          .map {
            value =>
                val hds = fs2.kafka.Headers.fromIterable(value.headers.map(
                  (k, v) => fs2.kafka.Header(k, v.getBytes(StandardCharsets.UTF_8))
                ))
                println(s"Sending message to topic '${value.topic}' with key '${value.key}'")
                val record = ProducerRecord(value.topic, value.key, value.value).withHeaders(hds)
                ProducerRecords.one(record)
          }
          .through(KafkaProducer.pipe(producerSettings))
          .compile
          .drain

    def produce(value: ProducerParams): IO[Either[Channel.Closed, Unit]] = queue.send(value)

class CustomDeserializer {

  val kafkaAvroSerDeConfig = Map[String, Any](
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> "http://localhost:18081",
    AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS -> "true",
    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString,
    AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> "registry.strategy.RecordSubjectStrategy",
    AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION -> false.toString,
  )

  val kafkaAvroDeserializer = new KafkaAvroDeserializer()
  kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, false)

  val deserializer = kafkaAvroDeserializer.asInstanceOf[KDeserializer[SpecificRecord]]
}

class ConsumerImpl(deserializer: KDeserializer[SpecificRecord]):

    def processRecord(record: ConsumerRecord[String, SpecificRecord]): IO[Unit] = IO(
      println(s"Processing record: ${record.value.asInstanceOf[org.integration.avro.transactions.CreditRequest]}")
    )
    // IO(println(s"Processing record: ${record}"))

    def init(): IO[Unit] =
        val consumerSettings = ConsumerSettings(
          keyDeserializer = Deserializer[IO, String],
          valueDeserializer = Deserializer.delegate(deserializer)
        ).withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withBootstrapServers("localhost:19092")
          .withGroupId("group")

        val stream = KafkaConsumer
          .stream(consumerSettings)
          .subscribeTo("topic")
          .records
          .mapAsync(2) {
            committable =>
              processRecord(committable.record).as(committable.offset)
          }
          .through(commitBatchWithin(5, 2.seconds))

        stream.compile.drain

class ConsumerImpl2(deserializer: KDeserializer[SpecificRecord]):

    def processRecord(record: CommittableConsumerRecord[IO, String, SpecificRecord]): IO[Unit] = IO(
      println(s"Processing record: ${record.record.value.asInstanceOf[org.integration.avro.transactions.CreditRequest]}")
    )
    // IO(println(s"Processing record: ${record}"))

    val consumerSettings = ConsumerSettings(
      keyDeserializer = Deserializer[IO, String],
      valueDeserializer = Deserializer.delegate(deserializer)
    ).withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers("localhost:19092")
      .withGroupId("group")

    def run(consumer: KafkaConsumer[IO, String, SpecificRecord]): IO[Unit] =
      consumer
        .subscribeTo("topic") >> consumer
          .stream
          .evalMap {
            msg =>
              processRecord(msg).as(msg.offset)
          }
          .through(commitBatchWithin(5, 2.seconds))
          .compile
          .drain
