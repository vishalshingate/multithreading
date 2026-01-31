# Using Avro Serializer/Deserializer in Spring Boot Kafka

We can definitely use Avro Serializer/Deserializer instead of the default String/JSON serializers. This involves using the **Confluent Schema Registry** to manage schemas.

## Why use Avro?
1.  **Compactness**: Avro is a binary format, so messages are much smaller than JSON.
2.  **Schema Evolution**: It enforces a schema structure and allows compatible changes (e.g., adding fields with default values) without breaking existing consumers.
3.  **Type Safety**: Generates Java classes from schema files.

## Implementation Steps

### 1. Add Confluent Repository
Since Avro libraries are hosted by Confluent, add their repository to `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven { url "https://packages.confluent.io/maven/" }
}
```

### 2. Add Dependencies
Add the following dependencies in `build.gradle`:

```groovy
dependencies {
    // ... existing dependencies
    implementation 'io.confluent:kafka-avro-serializer:7.4.0'
    implementation 'org.apache.avro:avro:1.11.1'
}
```

### 3. Add Avro Plugin (Optional but Recommended)
To automatically generate Java classes from `.avsc` files, use a Gradle plugin.

```groovy
plugins {
    // ... existing plugins
    id "com.github.davidmc24.gradle.plugin.avro" version "1.8.0"
}
```

### 4. Create Avro Schema
Create a schema file at `src/main/avro/LibraryEvent.avsc`:

```json
{
  "namespace": "com.learnkafka.libraryevents",
  "type": "record",
  "name": "LibraryEvent",
  "fields": [
    {
      "name": "libraryEventId",
      "type": "int"
    },
    {
      "name": "libraryEventType",
      "type": {
        "type": "enum",
        "name": "LibraryEventType",
        "symbols": ["NEW", "UPDATE"]
      }
    },
    {
      "name": "book",
      "type": {
        "type": "record",
        "name": "Book",
        "fields": [
          {"name": "bookId", "type": "int"},
          {"name": "bookName", "type": "string"},
          {"name": "bookAuthor", "type": "string"}
        ]
      }
    }
  ]
}
```

### 5. Configure `application.yaml`
Update the consumer configuration to use the Avro deserializer.

```yaml
spring:
  kafka:
    consumer:
        key-deserializer: org.apache.kafka.common.serialization.IntegerDeserializer
        # Change value-deserializer to Avro
        value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
        properties:
          schema.registry.url: http://localhost:8081
          specific.avro.reader: true # To map to specific Java class instead of GenericRecord
    producer:
      key-serializer: org.apache.kafka.common.serialization.IntegerSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://localhost:8081
```

### 6. Update Consumer Code
Change the listener method signature to accept the generated Avro object.

```java
@KafkaListener(topics = {"library-events"})
public void onMessage(ConsumerRecord<Integer, LibraryEvent> consumerRecord) {
    log.info("ConsumerRecord : {} ", consumerRecord);
    // ... logic
}
```

---

## Scenario-Based Interview Questions

**Q1: What is the role of Schema Registry?**
**A:** Kafka is unaware of the message structure (it sees bytes). The Schema Registry stores the schema (Avsc) separately. The producer sends the Schema ID in the message header. The consumer uses this ID to fetch the schema from the registry and deserialize the message. This decouples the schema from the message payload.

**Q2: What is "SpecificRecord" vs "GenericRecord"?**
**A:** 
- **SpecificRecord**: Java classes generated from `.avsc` files. Provides compile-time type safety (e.g., `event.getBookName()`).
- **GenericRecord**: Works like a map/JSON structure. Used when the schema is not known at compile time or you don't want to generate classes.

**Q3: How do you handle Schema Evolution? (Backward/Forward Compatibility)**
**A:** 
- **Backward Compatibility**: New schema can read old data. (e.g., deleting a field). Consumer upgraded first.
- **Forward Compatibility**: Old schema can read new data. (e.g., adding a field). Producer upgraded first.
- **Full Compatibility**: Both are supported.
*Scenario*: If you add a mandatory field to the schema, it breaks compatibility. You must add it with a `default` value to maintain compatibility.

**Q4: How does Avro serialization reduce network bandwidth?**
**A:** Avro does not send field names in every message (unlike JSON). It only sends the values packed in binary. The field names and types are looked up from the Schema Registry using the Schema ID sent with the message. This makes the payload significantly smaller.

**Q5: What happens if Schema Registry is down?**
**A:** 
- **Producer**: Cannot register new schemas or validate existing ones. Will fail to produce messages if schemas are not cached.
- **Consumer**: Cannot fetch schemas to deserialize messages. Will fail to consume.
*Mitigation*: Schema Registry clients cache IDs locally, so short outages might not affect high-throughput systems immediately, but it is a critical component.
