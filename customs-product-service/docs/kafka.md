# Kafka

## Topics
### Adding a topic
We must create a topic before we produce or consume a message. We have CD pipeline provisioning the topics.

#### Local
The topics can be configured in the `docker-compose.yaml`. The topics should be created after rerunning docker containers.
```yaml
init-kafka-topic:
    TOPICS: "service-name.a.protos service-name.b.protos"
```

#### Staging and Production
The topics can be configured in the `server/fsm-resources/manifest.yaml`. Then we have workflows picking up the topics deployment by calling the `fsm-resource-deploy` github action. You can find more configuration details [here](https://flexport.atlassian.net/wiki/spaces/IN/pages/2299658370/How+to+set+up+and+deploy+FSM+resources#Topic-configurations) .
```yaml
topics:
  - name: service-name.a.protos
    partitions: 3
    replicas: 2
  - name: service-name.b.protos
    partitions: 1
    replicas: 2
```

### Naming conventions

For customs projects follow the convention from
[ADR-4](https://github.com/flexport/customs-adr/blob/main/decisions/accepted/0004-naming-and-versioning-convention-for-kafka-topics.md)

### Viewing a topic
#### Local
Topics can be viewed via [Kafka UI](http://localhost:8092) after running docker containers.

#### Dev
Topics can be viewed via [Kafka UI](https://dev-fsm-kafka-ui.dev.engdev03.flexport.internal). We don't deploy the topics to dev cluster just yet.

#### Staging
Topics can be viewed via [Kafka UI](https://staging-fsm-kafka-ui.staging.engdev03.flexport.internal) after deploying to staging cluster.

#### Production
Topics can be viewed via [Kafka UI](https://fsm-kafka-ui.engprod03.flexport.internal) after deploying to production cluster. Note `aws_engdev03_ViewOnlyAccess` entitlement is needed to access Kafka UI.

## Producers

### Adding a producer

`SimpleProducer` is a simple producer.
It should be instantiated during the lifetime of the application, optionally lazily.

To add a `"myTopicProducer"` you need to add the following config to the `configuration.yaml` `kafka.producers` section:

```yaml
kafka:
  producers:
    myTopicProducer:
      topic: my.topic
      # See KafkaProducerConfiguration for additional configuration options
```

Implement the producer:

```kotlin
class MyTopicProducer(config: KafkaConfiguration) {
  private val producer = simpleProducer<String, MyProtobufMessage>(config, name)

  suspend fun send(msg: MyProtobufMessage) {
    producer.send(msg.id, msg)
  }

  companion object {
    const val name = "myTopicProducer"
  }
}
```

And add it to your `kafkaProducerModule`:

```kotlin
val kafkaProducerModule = module {
  singleOf(::MyTopicProducer)
}
```

#### Specifying the serializer

By default, the following message will be serialized in the following format:
1. String, UUID, Integer and others provided by the Kafka JVM client will use those formats
2. Protobuf will be serialized as binary protobuf, using schema registry if configured
3. All other types will be serialized as JSON using the Jackson `JsonMapper` instance registered in Koin.

You can explicitly pass in serializers also:

```kotlin
simpleProducer<String, MyProtobufMessage>(
  config,
  name,
  // Serialize the key as a JSON-wrapped string
  jsonSerializer(),
  // Serialize the value as protobuf with schema registry
  schemaRegistryProtoSerializer(),
)

// All options:
val builtin = StringSerializer::class
val protobuf = protoSerializer()
val protobufWithSchemaRegistry = schemaRegistryProtoSerializer()
val json = jsonSerializer()
```

## Consumers

Once registered, all `Consumer`s are automatically picked up and injected in the `KafkaConsumerManager`.
The `KafkaConsumerManager` manages the life-cycle of the all consumers. It's a single component that gets running on
server started and gets stopped on server shutdown.

### Adding a consumer

`SimpleConsumer` is a consumer that should "do the right thing" in most cases.
It can be created with the `simpleConsumer` builder.

To add a `"myTopicConsumer"` you need to add the following config to `configuration.yaml` `kafka.consumers` section:

```yaml
kafka:
  consumers:
    myTopicConsumer:
      topic: my.topic
      # See KafkaConsumerConfiguration for additional configuration options
```

Implement the consumer:

```kotlin
class MyTopicConsumer(
  private val config: KafkaConfiguration,
) {

  fun create() = simpleConsumer<String, MyProtobufMessage>(config, name) {

    messageHandler { record ->
      log.info("Received $record")
    }
  }

  companion object {
    const val name = "myTopicConsumer"
  }
}
```

And add it to your `kafkaConsumerModule`:

```kotlin
val kafkaConsumerModule = module {
  single(named(MyTopicConsumer.name)) {
    MyTopicConsumer(get()).create()
  }.bind<Consumer>()
}
```

#### Specifying the deserializer

By default, the following message will be deserialized from the following format:
1. String, UUID, Integer and others provided by the Kafka JVM client will use those formats.
2. Protobuf will be read from binary protobuf. The schema registry wrapper will be ignored if it exists.
3. All other types will be deserialized from JSON using the Jackson `JsonMapper` instance registered in Koin.

You can explicitly configure in deserializers also:

```kotlin
simpleConsumer<String, MyProtobufMessage>(config, name) {

  setKeyDeserializer<StringDeserializer>()
  setValueDeserializerToSchemaRegistryProto()

  messageHandler { record ->
    // ...
  }
}

// All options:
// Kafka provided deserializers like StringDeserializer
set___Deserializer<StringDeserializer>()
// Protobuf deserializer that supports both plain and schema registry wrapped binary proto
set___DeserializerToProto()
// Protobuf schema registry deserializer
set___DeserializerToSchemaRegistryProto()
// JSON deserializer using the Jackson instance configure in Koin
set___DeserializerToJson()
```

## Schema Registry
Schema Registry provides the API that allows producers or consumers to predict whether the message they are about to produce/consume is compatible with previous versions. It supports JSON, Avro and Proto buffers.

### Viewing schemas
#### Local
- [List the schema types API](http://localhost:8085/schemas/types)
- [List the subjects API](http://localhost:8085/subjects)
- [Kafka UI](http://localhost:8092/ui/clusters/local/schemas)

## Kafka support in Monorail
The FSM team is currently building a Ruby client library to support Kafka functionalities in Monorail. In the meantime we have some workaround solutions.
### Producing messages in Core
We implement a `CargoManifest::MessageProducer` ([source code](https://github.com/flexport/flexport/blob/master/engines/cargo_manifest/app/services/cargo_manifest/message_producer.rb)) in Monorail.

```rb
CargoManifest::MessageProducer.produce(topic: topic, messages: messages)
```

### Consuming messages in Core
We consume our own messages and forward them over HTTP to Core for now.
We implement a `CargoManifest::MessageController` ([source code](https://github.com/flexport/flexport/blob/master/engines/cargo_manifest/app/controllers/cargo_manifest/message_controller.rb)) as an endpoint of the Core Internal API. The endpoint gets called from `MessageForwarder`.


##Health Checks

Can be enabled by adding `kafka` to the `health.probes.liveness` list in `configuration.yaml`.

note: enabling this probe will cause your application to be rebooted if Kafka connection fails, so it will take down your GraphQL, HTTP and/or gRPC server on Kafka troubles. So before enabling
this probe consider wether your service can function with Kafka.
