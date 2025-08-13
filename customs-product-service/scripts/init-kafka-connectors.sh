#!/usr/bin/env bash

# shellcheck disable=SC2016

# customs-product-service-connector
curl -X DELETE 'http://localhost:8083/connectors/customs-product-service-connector'
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '
{ "name": "customs-product-service-connector",
  "config":
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
 }'

# customs-product-service-outbox-connector
curl -X DELETE 'http://localhost:8083/connectors/customs-product-service-outbox-connector'
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '
{ "name": "customs-product-service-outbox-connector",
  "config":
   {
     "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
     "publication.name": "dbz_outbox_publication",
     "slot.name": "dbz_outbox_publication",
     "plugin.name": "pgoutput",
     "database.hostname": "postgres",
     "database.port": "5432",
     "database.user": "develop",
     "database.password": "develop_secret",
     "database.dbname" : "customs_product_service_develop",
     "database.server.name": "postgres",
     "table.include.list": "public.outbox",
     "topic.prefix": "customs-product-service-outbox-connector",
     "snapshot.mode": "initial",
     "auto.create": "true",
     "transforms": "outbox",
     "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
     "transforms.outbox.route.by.field": "topic",
     "transforms.outbox.route.topic.replacement": "${routedByValue}",
     "transforms.outbox.table.field.event.key": "key",
     "transforms.outbox.table.field.event.payload": "value",
     "transforms.outbox.table.fields.additional.placement": "created_at:header:created_at,datadog_parent_id:header:x-datadog-parent-id,datadog_trace_id:header:x-datadog-trace-id",
     "key.converter": "io.debezium.converters.BinaryDataConverter",
     "value.converter": "io.debezium.converters.BinaryDataConverter"
   }
 }'
