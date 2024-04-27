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

    import util.TransportError
    // import util.Result
    import akka.actor.typed.{ ActorSystem => TypedActorSystem }

    def register(sys: TypedActorSystem[?]): ObjectMapper =
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

        // module.addSerializer(new ErrorOr_A_Serializer())
        // module.addDeserializer(classOf[ErrorOr[CborSerializable]], new ErrorOr_A_Deserializer())

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

    // class ErrorOr_A_Serializer extends StdSerializer[ErrorOr[CborSerializable]](classOf[ErrorOr[CborSerializable]]):

    //     override def serialize(value: ErrorOr[CborSerializable], gen: JsonGenerator, provider: SerializerProvider): Unit =
    //         gen.writeStartObject()
    //         val mapper = gen.getCodec().asInstanceOf[ObjectMapper]
    //         value match
    //             case Left(err: ResultError)  =>
    //                 gen.writeStringField("_typeBase", "left")
    //                 // gen.writeStringField("_type", err.getClass().getSimpleName())
    //                 val rawValue = mapper.writeValueAsBytes(err)
    //                 // gen.writeBinaryField("value", rawValue)
    //                 gen.writeBinary(rawValue)
    //             case Right(data: CborSerializable) =>
    //                 gen.writeStringField("_typeBase", "right")
    //                 // gen.writeStringField("_type", data.getClass().getSimpleName())
    //                 val rawValue = mapper.writeValueAsBytes(data)
    //                 gen.writeBinaryField("value", rawValue)
    //                 // gen.writeBinary(rawValue)

    //         // val rawValue = mapper.writeValueAsBytes(value)
    //         // gen.writeBinary(rawValue)
    //         gen.writeEndObject()

    // class ErrorOr_A_Deserializer extends StdDeserializer[ErrorOr[CborSerializable]](classOf[ErrorOr[CborSerializable]]):

    //     override def deserialize(p: JsonParser, ctxt: DeserializationContext): ErrorOr[CborSerializable] =
    //         val node = p.getCodec().readTree(p).asInstanceOf[JsonNode]
    //         val mapper = p.getCodec().asInstanceOf[ObjectMapper]
    //         val typeBase = node.get("_typeBase").asText()
    //         // val _type = node.get("_type").asText()
    //         val value = node.get("value").binaryValue()
    //         typeBase match
    //             case "left" =>
    //                 // val clazz = Class.forName(_type)
    //                 // val obj = mapper.readValue(value, clazz)
    //                 val obj = mapper.re(value)
    //                 Left(obj.asInstanceOf[ResultError])
    //             case "right" =>
    //                 val clazz = Class.forName(_type)
    //                 val obj = mapper.readValue(value, clazz)
    //                 Right(obj.asInstanceOf[CborSerializable])
