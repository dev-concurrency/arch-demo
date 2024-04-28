package registry.strategy;

import io.confluent.kafka.schemaregistry.ParsedSchema;

public class NoSubjectException extends FrameworkException {

    public NoSubjectException(ParsedSchema schema) {
      super(String.format("A 'subject' is required within the following RecordSchema: %s",
          schema.canonicalString()));
    }
}
