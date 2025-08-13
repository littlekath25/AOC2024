#!/bin/bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

DBNAME='customs_product_service_develop'
CONNSTRING='postgresql://develop:develop_secret@localhost:8765'

# Create DBNAME if it doesn't exist
if [[ $( psql "$CONNSTRING/postgres" -tAc "SELECT COUNT(*) FROM pg_database WHERE datname = '$DBNAME'") == "0" ]]; then
  psql "$CONNSTRING/postgres" -c "CREATE DATABASE $DBNAME"
  psql -v ON_ERROR_STOP=1 "$CONNSTRING/$DBNAME" <<- SQL
      GRANT CONNECT ON DATABASE $DBNAME TO reader;
      ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT ON TABLES TO reader;
SQL
fi

cd "$SCRIPT_DIR/../server"
FLYWAY_CLEAN_DISABLED=false ../gradlew flywayClean flywayMigrate generateJooq
