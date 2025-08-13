#!/usr/bin/env bash
#
# This script is specifically used for init-kafka-topic docker container defined in docker-compose.yaml.
# It creates the topics at kafka startup.
#

# Split string into an array
IFS=', ' read -r -a topics_array <<< "$TOPICS"

# Block until broker is reachable
echo -e 'Current topics:'
kafka-topics --bootstrap-server "$BROKER" --list

echo -e 'Creating kafka topics:'
for topic in "${topics_array[@]}"
do
  echo "Going to create kafka topic: $topic"
  kafka-topics --bootstrap-server "$BROKER" --create --if-not-exists --topic "$topic" --replication-factor 1 --partitions 3
  echo "Finished creating kafka topic: $topic"
done

echo -e 'Successfully created the following topics:'
kafka-topics --bootstrap-server "$BROKER" --list
