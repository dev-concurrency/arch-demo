package registry.strategy;


import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import registry.strategy.NoSubjectException;
import registry.strategy.NotSupportedSchemaException;

import java.util.Map;
import java.util.Optional;
import org.apache.avro.Schema;

public class RecordSubjectStrategy implements SubjectNameStrategy {

  public static final String SUBJECT_PROPERTY = "subject";
  private static final String RECORD_SCHEMA_CLASS = "RecordSchema";

  @Override
  public void configure(Map<String, ?> config) {
  }

  @Override
  public boolean usesSchema() {
    return true;
  }

  @Override
  public String subjectName(String topic, boolean isKey, ParsedSchema schema){
    Schema rawSchema = Optional.ofNullable(schema)
        .map(ParsedSchema::rawSchema)
        .filter(Schema.class::isInstance)
        .map(Schema.class::cast)
        .filter(raw -> String.valueOf(raw.getClass()).endsWith(RECORD_SCHEMA_CLASS))
        .orElseThrow(() -> new NotSupportedSchemaException(schema));

    return Optional.ofNullable(rawSchema.getProp(SUBJECT_PROPERTY))
        .orElseThrow(() -> new NoSubjectException(schema));
  }

}
