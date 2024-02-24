package infrastructure

object Serializers:

    import com.fasterxml.jackson.core.JsonGenerator
    import com.fasterxml.jackson.core.JsonParser
    import com.fasterxml.jackson.databind.DeserializationContext
    import com.fasterxml.jackson.databind.SerializerProvider
    import com.fasterxml.jackson.databind.deser.std.StdDeserializer
    import com.fasterxml.jackson.databind.ser.std.StdSerializer

    import com.fasterxml.jackson.databind.ObjectMapper
    import akka.serialization.jackson.JacksonObjectMapperProvider
    import com.fasterxml.jackson.databind.module.SimpleModule

    import persistence.util.TransportError
    import akka.actor.typed.{ ActorSystem => TypedActorSystem }

    def register(sys: TypedActorSystem[_]): ObjectMapper =
        val mapper: ObjectMapper = JacksonObjectMapperProvider(sys).getOrCreate("jackson-cbor", None)
        val mapperJson: ObjectMapper = JacksonObjectMapperProvider(sys).getOrCreate("jackson-json", None)
        val module: SimpleModule = new SimpleModule()

        // mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        // mapperJson.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        module.addSerializer(new TransportErrorSerializer())
        module.addDeserializer(classOf[TransportError], new TransportErrorDeserializer())

        module.addSerializer(new LogbackInfoStatusSerializer())
        module.addDeserializer(classOf[ch.qos.logback.core.status.InfoStatus], new LogbackInfoStatusDeserializer())

        module.addSerializer(new LogbackLoggerContextSerializer())
        module.addDeserializer(classOf[ch.qos.logback.classic.LoggerContext], new LogbackLoggerContextDeserializer())

        module.addSerializer(new AkkaDoneSerializer())
        module.addDeserializer(classOf[akka.Done], new AkkaDoneDeserializer())

        mapper.registerModule(module)
        mapperJson.registerModule(module)

    class TransportErrorSerializer extends StdSerializer[TransportError](classOf[TransportError]):
        import TransportError._

        override def serialize(value: TransportError, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue =
              value match
                case NotFound            => "NF"
                case BadRequest          => "BR"
                case InternalServerError => "IS"
                case Unauthorized        => "UA"
                case Forbidden           => "FB"
                case Maintenance         => "MT"
                case Unknown             => "UN"
            gen.writeString(strValue)

    class TransportErrorDeserializer extends StdDeserializer[TransportError](classOf[TransportError]):
        import TransportError._

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): TransportError =
          p.getText match
            case "NF" => NotFound
            case "BR" => BadRequest
            case "IS" => InternalServerError
            case "UA" => Unauthorized
            case "FB" => Forbidden
            case "MT" => Maintenance
            case "UN" => Unknown

    class LogbackInfoStatusSerializer
        extends StdSerializer[ch.qos.logback.core.status.InfoStatus](classOf[ch.qos.logback.core.status.InfoStatus]):

        override def serialize(value: ch.qos.logback.core.status.InfoStatus, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue = ""
            gen.writeString(strValue)

    class LogbackInfoStatusDeserializer
        extends StdDeserializer[ch.qos.logback.core.status.InfoStatus](classOf[ch.qos.logback.core.status.InfoStatus]):

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): ch.qos.logback.core.status.InfoStatus =
          new ch.qos.logback.core.status.InfoStatus("", null)

    class LogbackLoggerContextSerializer
        extends StdSerializer[ch.qos.logback.classic.LoggerContext](classOf[ch.qos.logback.classic.LoggerContext]):

        override def serialize(value: ch.qos.logback.classic.LoggerContext, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue = ""
            gen.writeString(strValue)

    class LogbackLoggerContextDeserializer
        extends StdDeserializer[ch.qos.logback.classic.LoggerContext](classOf[ch.qos.logback.classic.LoggerContext]):

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): ch.qos.logback.classic.LoggerContext =
          new ch.qos.logback.classic.LoggerContext()

    // java.util.concurrent.ConcurrentHashMap
    // class Serializer extends StdSerializer[](classOf[]):

    //     override def serialize(value: ch.qos.logback.core.status.InfoStatus, gen: JsonGenerator, provider: SerializerProvider): Unit =
    //         val strValue = ""
    //         gen.writeString(strValue)

    // class Deserializer extends StdDeserializer[](classOf[]):

    //     override def deserialize(p: JsonParser, ctxt: DeserializationContext):  =
    //         ???

    class AkkaDoneSerializer extends StdSerializer[akka.Done](classOf[akka.Done]):

        override def serialize(value: akka.Done, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue = "Done"
            gen.writeString(strValue)

    class AkkaDoneDeserializer extends StdDeserializer[akka.Done](classOf[akka.Done]):

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): akka.Done = akka.Done
