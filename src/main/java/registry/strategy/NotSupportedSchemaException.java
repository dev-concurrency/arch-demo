package registry.strategy;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.Optional;

public class NotSupportedSchemaException extends FrameworkException {
  public NotSupportedSchemaException(ParsedSchema schema) {
    super(
        String.format("'%s' was provided. Only 'org.apache.avro.Schema#RecordSchema' is supported!",
            Optional.ofNullable(schema)
                .map(ParsedSchema::rawSchema)
                .map(Object::getClass)
                .map(String::valueOf)
                .map(schStr -> schStr.replaceAll("class ", ""))
                .orElse(null)));
  }
}
