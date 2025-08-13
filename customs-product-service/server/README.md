# Server side

The service uses

- Postgres as the database, run via Docker `docker compose up -d` to get it going
- Flyway to manage the database schema migrations
- jOOQ to generate the DSL to work with the database

Use `../scripts/generate-db-schema.sh` to clear the database, set the schema,
and generate classes with jOOQ.

In IntelliJ, sync the Gradle module and pray that it finds the generated
code dirs automatically. If it does not, add them manually for code to work.
