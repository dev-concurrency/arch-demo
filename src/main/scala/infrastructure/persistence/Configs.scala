package infrastructure
package persistence

object Configs:

    case class KafkaConfig
      (
        bootstrapServers: String,
        schemaRegistryUrl: String,
        autoRegisterSchemas: String,
        valueSubjectNameStrategy: String)
