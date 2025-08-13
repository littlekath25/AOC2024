# Kafka Connect

Kafka connect allows us to capture changes applied to tables in postgresql, and automatically publish them as messages on a kafka topic. It also allows us to to export data from kafka topics to Snowflake tables.

## Table of Contents
- [Postgres Source Connector](#postgres-source-connector)
  - [Local Environment](#local-environment)
  - [Staging/Production Environment](#stagingproduction-environment)
  - [Troubleshooting](#troubleshooting)
- [Snowflake Sink Connector](#snowflake-sink-connector)

## Postgres Source Connector

### Local Environment

#### Starting the CDC
Each service comes with a `docker-compose.yaml` file, which configures a local version of kafka connect.

Kafka connect initializes along with the rest of the project `docker compose up`.

Run the `scripts/init-kafka-connectors.sh` to initialize change data capture on the desired tables. If you are using
IntelliJ there is an included run action.
![Run Init Kafka Connectors](images/run-init-kafka-connectors.png)

If the connectors are created successfully you should see the following message in console:
![Success Created Connectors](images/created-connectors.png)

If the script returns `curl: (52) Empty reply from server`, kafka connect container may not be fully initialized.
You can verify by checking the rest API:
```shell
curl --fail -H "Accept:application/json" localhost:8083/
```

#### Viewing the Connectors

You will be able to see a listing of your connectors here:
[http://localhost:8092/ui/clusters/local/connectors](http://localhost:8092/ui/clusters/local/connectors)

In the local dev environment, topics will auto-create and can be seen here:
[http://localhost:8092/ui/clusters/local/all-topics](http://localhost:8092/ui/clusters/local/all-topics)

**NOTE**: Connector configurations in the local environment persist between restarts. If you end up with a strange configuration, you will need to delete the container and volume to reset completely.

#### Adding a new CDC Table

To add a new table for change data capture, edit `scripts/init-kafka-connectors.sh` `table.include.list` by adding a
table to the list. Kafka connect expects this to be a list of regular expressions. Most probably you will want to
have a list of exact matches.

```json
 {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "develop",
    "database.password": "develop_secret",
    "database.dbname" : "customs_product_service_develop",
    "database.server.name": "postgres",
    "table.include.list": "public.table_name",
    "topic.prefix": "customs-product-service-connector",
    "snapshot.mode": "initial",
    "auto.create": "true",
    "publication.name": "dbz_publication",
    "slot.name": "dbz_publication"
 }
```

You will need create a new connector only if you need to connect to another database or need a new configuration
(for example `snapshot.mode`).

#### Example code
- postgres connector setup in [customs-log-aggregator](https://github.com/flexport/customs-log-aggregator/blob/2d3b7bb2ecfa329d7ada15330b53469e9d833ac1/scripts/init-kafka-connectors.sh#L15)

### Staging/Production Environment
Kafka topics and connectors are deployed via FSM provided shared github action workflows. The configuration for topics and connectors is stored in the `server/fsm-resources/manifest.yaml` that will be deployed to staging and production via normal channels.

#### Configuring `postgres_connector_secrets_arn`
```yaml
postgres_connector_secrets_arn:
  environments:
    - staging: arn:aws:secretsmanager:us-east-1:****:secret:infra/staging/core_staging/cargo-manifest-query/POSTGRES_DBZ-15z2pD
    - prod: arn:aws:secretsmanager:us-east-1:****:secret:infra/production/default/cargo-manifest-query/POSTGRES_DBZ-hn7uHd
postgres_connector_secrets:
  - connector_name: cmq-connector
    database_password_sm_key: PASSWORD
```

To get the relevant secrets configuration, look in https://backstage.gess.flexport.internal/secrets . The NGIF service template will generated the relevant ARN for user `POSTGRES_DBZ`. The `database_password_sm_key` will always be `PASSWORD`.

#### Configuring `debezium_postgres_connectors`
```yaml
debezium_postgres_connectors:
  - name: [service_name]-connector
    database_username: [service_name]_dbz
    database_name: [service_name]
    schema_includelist: public
    table_includelist: public.[table_name]
    slot_name: dbz_publication
    plugin_name: pgoutput
    environments:
      - staging:
          database_hostname: [service_name]-core_staging.engdev03.flexport.internal
      - prod:
          database_hostname: [service_name].engprod03.flexport.internal
```
1. Name can be set to anything, `service-name-connector` might be a good pattern to follow.

2. `database_username` and `database_name` will always be based on the `service_name`

3. `schema_includelist` refers to the postgresql schema (usually this is `public` by default)

4. `slot_name` is always `dbz_publication`

5. `plugin_name` is always `pgoutput`

6. `database_hostname` is determined based on service name and is set by [tf-aurora-mod-postgresql](https://github.com/flexport/tf-mod-aurora-postgresql)

7. The destination topic will be `[connector_name].[schema].[table_name]`

#### Viewing the Connectors
There is [staging UI](https://staging-fsm-kafka-ui.staging.engdev03.flexport.internal/ui/clusters/staging-fsm/connectors) and [production UI](https://fsm-kafka-ui.engprod03.flexport.internal/ui/clusters/fsm/connectors) that can be used to verify the connector has started up in the environment and that data is flowing into the topic.

#### Example code
- postgres connector setup in [customs-log-aggregator](https://github.com/flexport/customs-log-aggregator/blob/2d3b7bb2ecfa329d7ada15330b53469e9d833ac1/server/fsm-resources/manifest.yaml#L46-L76)

### Troubleshooting

## My source connector Kafka topic is not receiving any messages on Production! How should I debug?

When no messages are appearing on your Kafka topic, check wether the kubernetss kafkaconnector resource is succesfully created.

1. Log in to the fsm k8s namespace using the `data-infrastructure-breakglass` role

```
> tsh login --proxy teleport-proxy.flexport.io --request-roles=data-infrastructure-breakglass --request-reason=<REASON TO REQUEST ROLE>
```

2. List all the kafkaconnectors & check if your connector is ready

```
> kubectl get kctr --namespace fsm
NAME                                                                             CLUSTER                     CONNECTOR CLASS                                               MAX TASKS   READY
......
customs-ca-adapter-connector                                                     source-connectors-cluster   io.debezium.connector.postgresql.PostgresConnector            1           True
customs-ca-adapter-snowflake-connector                                           sink-connectors-cluster     com.snowflake.kafka.connector.SnowflakeSinkConnector          3           
......
```

3. If your connector is not ready (READY column shows FALSE or is empty) use kubectl describe it to get all recent events associated with the kafkaconnector. This should point you to the error

```
> kubectl describe kctr customs-ca-adapter-snowflake-connector    --namespace fsm
........
    Message:               PUT /connectors/customs-ca-adapter-snowflake-connector/config returned 400 (Bad Request): Connector configuration is invalid and contains the following 1 error(s):
Invalid value capital-connector.public.application:application, capital-connector.public.lead:lead, capital-connector.public.repayment_schedule:repayment_schedule capital-connector.public.repayment_schedule_payment:payment capital-connector.public.repayment_schedule_refund:refund for configuration snowflake.topic2table.map: Format: <topic-1>:<table-1>,<topic-2>:<table-2>,...
You can also find the above list of errors at the endpoint `/connector-plugins/{connectorType}/config/validate`
```

in this case the manifest was invalid.

4. If the kafkaconnector is ready probably something is wrong in the fsm-source-connector. You can view the logs in loki using the following query

```
{job="fsm/source-connectors-cluster-connect"} |= `<SERVICE_NAME>`
```

If something is wrong some warn/error logs will pop up which should pinpoint you to an underlying issue


###### Connector and Topic not Appearing After Initialization

- If you initialize the connectors prior to the generating the schema, you get a success message of the connector being
created, but not be able to view it in the UI.

   ```shell
   docker-compose --project-directory server up -d
   ./scripts/generate-db-schema.sh
   ./scripts/init-kafka-connectors.sh
   ```

- It can also be the case that no data exists in the db table, try inserting the data, you should be able to see the topic appear in the UI.

## Snowflake Sink Connector
Snowflake connectors are configured through the same `server/fsm-resources/manifest.yaml` file as postgres source connectors.

```yaml
snowflake_connectors:
  - name: [service-name]-snowflake-connector
    tasks_max: 3
    topics: >-
      [connector_name].[schema].[table_name],
      [connector_name].[schema].[table_name],
    topic2table_map: >-
      [connector_name].[schema].[table_name]:[table_name],
      [connector_name].[schema].[table_name]:[table_name]
    value_converter: org.apache.kafka.connect.storage.StringConverter
    key_converter: org.apache.kafka.connect.storage.StringConverter
    environments:
      - staging:
          schema_name: FSM_[SERVICE_NAME]_STAGING
      - prod:
          schema_name: FSM_[SERVICE_NAME]
```

1. Name the snowflake connector with the service
2. A snowflake sink connector can map any topic to any table, however, in this example the topic would be set by the postgresql connector
3. Give the connector a separate schema name for staging and prod. There is only one instance of Snowflake, the data will be separated by schema name.

Follow the instructions in the [FSM Snowflake Schema Setup](https://flexport.atlassian.net/wiki/spaces/IN/pages/2591588473) to create the landing schema and configure Snowflake to receive the data.

#### Example code
- postgres connector setup in [customs-log-aggregator](https://github.com/flexport/customs-log-aggregator/blob/2d3b7bb2ecfa329d7ada15330b53469e9d833ac1/server/fsm-resources/manifest.yaml#L78-L106)
