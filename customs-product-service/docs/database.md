# Database

* [Access to production database](#access-to-production-database)
* [Access to local database](#access-to-local-database)
* [Database migrations](#database-migrations)
* [Database access from Kotlin](#database-access-from-kotlin)
  + [Code generation](#code-generation)
    - [Directly use the database definition of the entity](#directly-use-the-database-definition-of-the-entity)
    - [Define your own corresponding entity](#define-your-own-corresponding-entity)
    - [Updating Enums](#updating-enums)
  + [Examples](#examples)
    - [Select from a table](#select-from-a-table)
      * [Selecting the complete entity using the DAO and generated entity](#selecting-the-complete-entity-using-the-dao-and-generated-entity)
      * [Selecting the complete entity using the DAO and your own entity](#selecting-the-complete-entity-using-the-dao-and-your-own-entity)
      * [Selecting the complete entity with the DSL](#selecting-the-complete-entity-with-the-dsl)
      * [Selecting a subset of fields](#selecting-a-subset-of-fields)
    - [Insert or upsert](#insert-or-upsert)
      * [Using the generated interface and record](#using-the-generated-interface-and-record)
      * [Using the DSL](#using-the-dsl)
    - [Update](#update)

<!-- ToC can be updated with http://ecotrust-canada.github.io/markdown-toc/ -->

## Access to production database

You can access the staging and production database through Cyral.
[See this runbook on how to access a database with it](https://flexport.atlassian.net/wiki/spaces/~586662199/pages/2192572749/Connecting+with+Psql+using+Cyral).
Make sure you have Cyral assigned to you in Okta and otherwise submit a request to IT.

You can select read-write access in staging through `customs-product-service-readwrite-staging`, and
read-only access for production through `customs-product-service-readonly-production`.
In the psql command from Cyral you will need to replace `-d <DATABASE>` with `-d customs_product_service`.

For break-glass-in-case-of-emergency situations [you can request temporary production read-write access
through the Cyral Slackbot](https://flexport.atlassian.net/wiki/spaces/IS/pages/2230714452/Just-in-time+Cyral+Access+User+Guide+via+Slackbot).

## Access to local database
When running the application on your PC (with docker compose and Application.kt Main) you can connect to
local DB inside the docker with the command
```
PGPASSWORD=develop_secret  psql -h localhost -p 8765 -U develop -d customs_product_service_develop
```

## Database migrations

We use [Flyway](https://flywaydb.org/) to perform database migration.
See [the README file in the migrations folder for more information](../server/src/main/resources/db/migration/README.md).

## Database access from Kotlin

[jOOQ](https://www.jooq.org/) is used for database access library in this template.
It is primarily
a [<abbr title="Domain Specific Language">DSL</abbr>](https://www.jooq.org/doc/latest/manual/sql-building/dsl-api/) to
write SQL queries in code.

jOOQ introspects the DB schema and generates Kotlin code corresponding to it to make this typesafe.
Additionally, for tables with a single primary key simple entities
and [<abbr title="Database Access Object">DAO</abbr>s](https://www.jooq.org/doc/latest/manual/sql-execution/daos/) are
generated to support simple CRUD operations.

### Code generation

Jooq will automatically generate code based on your database schema to improve IDE support and make database code
type-safe
through compiler checks.
This is done as part of the `generateJooq` gradle task, which runs before compilation and is executed as part
of `script/generate-db-schema.db`.

For a table `foo` with a primary key it will generate 4 things:

1. An interface `IFoo` with all fields nullable in `com.flexport.customsproductservice.db.tables.interfaces`
2. An immutable `data class Foo(...) : IFoo` with nullability matching your schema
   in `com.flexport.customsproductservice.db.tables.pojos`
3. A mutable `FooRecord: IFoo` with all fields nullable in `com.flexport.customsproductservice.db.tables.records`. It will also generate
   a limited version of this for tables without a primary key.
4. A <abbr title="Database Access Object">DAO</abbr> with query methods based on the primary key and other indexes
   in `com.flexport.customsproductservice.db.tables.daos`

For each table, you can utilize this in 2 ways:

#### Directly use the database definition of the entity

For simple entities, you can use the immutable data class (2) directly throughout your code.
This saves you an extra definition and writing conversion between your app and the database definition, so is also
slightly more efficient.

However, to make it easy to change your mind later and to make it clear you're using an entity generated from the
database,
it is advised to add a type alias to the package where you define your entities and primarily rely on this import:

```kotlin
typealias Foo = com.flexport.customsproductservice.db.tables.pojos.Foo
```

#### Define your own corresponding entity

If you want for any reason to define your own `data class Foo`, you can to that.

To still take advantage of the Jooq code generation, it needs to implement `IFoo` and 2 conversions need to be defined.
See [DatabasePojoConversion.kt](snippets/DatabasePojoConversion.kt) for an example.

#### Updating Enums

We will use an example of adding `AWESOME_EVENT` type into the list of `EventType` enum values.
To add a value into enum:

* Create a migration adding a value into postgres:
  `ALTER TYPE event_type ADD VALUE IF NOT EXISTS 'awesome_event'`
* Run migration with `./scripts/generate-db-schema.sh`
* Add `AWESOME_EVENT` into `server/src/main/kotlin/com/flexport/customsproductservice/app/enums/EventType.kt`
* Add a mapping into `server/src/main/kotlin/com/flexport/customsproductservice/database/converters/EventTypeConverter.kt`
  Example commit: de178fcc

### Advanced Code Generation

#### Custom Mappings and Forced Types

jOOQ allows for customization of the generated code and conversion functions using the `forcedType` object in the gradle
configuration. This allows us to use specific Kotlin types for database columns that correspond to them, improving the
type safety of our code. For example, for a String column corresponding to an FID, we can parse that directly into
and FID object automatically via jOOQ instead of having to map from String to FID in our code.

You can find detailed documentation for forced
types [here](https://www.jooq.org/doc/latest/manual/code-generation/codegen-advanced/codegen-config-database/codegen-database-forced-types/).

##### Mapping to custom Kotlin types

To map a column to a custom Kotlin type, we need to implement a jOOQ Converter and then add a `forcedType` to the
gradle configuration. The `forcedType` looks as follows:

```kotlin
ForcedType().apply {
  userType = "java.time.Instant"
  converter = "com.flexport.customs.lib.jooq.converters.InstantConverter"
  includeTypes = "TIMESTAMP"
}
```

##### Using JSONB columns

The NGIF template also includes out-of-the-box support for JSONB columns using Jackson. This allows for mapping any
Kotlin data class to a JSONB column and vice-verse. To enable this define a forced type with isAutoConverter set to
true:

```kotlin
ForcedType().apply {
  userType = "com.flexport.customsfilingservice.us.isffiling.database.types.IsfConditionContextDb"
  includeExpression = "importer_security_filing_condition.context"
  // This will use the NGIF jOOQ library CustomConverterProvider to serialize and deserialize objects using
  // the Jackson ObjectMapper configured in your application
  isAutoConverter = true
}
```

#### Renaming the generated database fields

Sometimes it is useful to prefix or suffix the automatically generated fields with a prefix or suffix to avoid name
conflicts with other types and increase clarity. For example, you might want all jOOQ generated enums to be suffixed
with `Db`. You can do this via the `strategy` jOOQ config as follows:

```kotlin
strategy.apply {
  name = "org.jooq.codegen.DefaultGeneratorStrategy"
  matchers = Matchers().apply {
    enums.apply {
      push(
        MatchersEnumType().apply {
          // This is a regex, so additional enums needing the postifx can be added
          expression = ".+(?<!_db)\$"
          enumClass = MatcherRule().apply {
            transform = MatcherTransformType.PASCAL
            // Make the enum name original_name_db which will become OriginalNameDb
            expression = "$0_db"
          }
        },
      )
    }
  }
}
```

### Examples

#### Select from a table

##### Selecting the complete entity using the DAO and generated entity

If your table has a primary key, and you need the complete data of an entity, this is the appropriate method to use.
See [the DSL example](#selecting-the-complete-entity-with-the-dsl) if your table does not have a primary key.
See [the subset example](#selecting-a-subset-of-fields) if you only need partial data, which will improve performance.

```kotlin
class FooRepository(dsl: DSLReadWriteContext) {
  private val fooDao = FooDao(dsl.configuration())

  fun getById(id: Int): Foo = fooDao.fetchOneById(id)
  fun getByBar(bar: String): List<Foo> = fooDao.fetchByBar(bar)
}
```

##### Selecting the complete entity using the DAO and your own entity

See [DatabasePojoConversion.kt](snippets/DatabasePojoConversion.kt) for a detailed example

##### Selecting the complete entity with the DSL

```kotlin
class FooRepository(private val dsl: DSLReadWriteContext) {

  fun getById(id: Int): Foo? =
    dsl
      .select(
        FOO.ID,
        FOO.BAR,
        FOO.BAZ,
      )
      .from(FOO)
      .where(FOO.ID.eq(id))
      .fetchOne()
      ?.into(Foo::class.java)
}
```

##### Selecting a subset of fields

```kotlin
class FooRepository(private val dsl: DSLReadWriteContext) {

  fun getSubsetByBars(ids: List<Int>): List<FooSubset> =
    dsl
      .select(
        FOO.BAZ,
        FOO.QUX,
      )
      .from(FOO)
      .where(FOO.ID.`in`(ids))
      .fetchInto(FooSubset::class.java)
}

data class FooSubset(val baz: Int, val qux: String)
```

#### Insert or upsert

##### Using the generated interface and record

```kotlin
class FooRepository(private val dsl: DSLReadWriteContext) {

  // This example assumes the respective DB DTO object inherits IFOO
  // If that's not the case you can use 'private fun Foo.toRecord(): FooRecord'
  private fun IFoo.toRecord(): FooRecord =
    dsl.write.newRecord(FOO).apply {
      from(this@toRecord)
      // If your entity has an auto-generated serial primary key, you also need to mark it as unchanged
      // Otherwise for the INSERT case, an error will occur due because jOOQ will try to insert a NULL.
      // For the UPDATE case, it does not matter, as the primary key cannot be changed this way anyway.
      changed(FOO.ID, false)
    }

  // Normal insert
  // for upsert, use .merge() instead of .insert()
  fun insertFoo(foo: Foo) {
    foo.toRecord().insert()
  }

  // Insert and return entity with generated ID
  // for upsert, use .merge() instead of .insert()
  // JOOQ only returns the primary key and sequence
  // (even if sequence is not the primary key)
  // use DSL method if you need to return other generated fields
  fun insertFooReturning(foo: Foo): Foo =
    foo
      .toRecord()
      .also { it.insert() }
      .let { foo.copy(id = it.id) }

  // Batch insert
  // for upsert, use .batchMerge()
  fun insertFoos(foo: List<Foo>) =
    dsl.write.batchInsert(foo.map { it.toRecord() }).execute()

  // Batch insert returning generated IDs
  // For upsert, use context.mergeInto instead of context.insertInto
  // JOOQ only returns the primary key and sequence
  // (even if sequence is not the primary key)
  // use DSL method if you need to return other generated fields
  fun insertFoosReturning(foos: List<Foo>): List<Foo> {
    if (foos.isEmpty()) return listOf()
    val records = foos.map { it.toRecord() }

    // Workaround for https://github.com/jOOQ/jOOQ/issues/3327
    val insertsBuilder = dsl.write.insertInto(FOO)
    // add the records one-by-one except for the last
    for (i in 0 until records.size - 1) {
      insertsBuilder.set(records[i]).newRecord()
    }
    val inserts = insertsBuilder.set(records.last()).returning(
      FOO.ID, FOO.FIELD1,
    ).fetch()

    return inserts.map { it.into(DbFoo::class.java).toFoo() }
  }
}

/** Convert the DB POJO to our entity. */
fun DbFoo.toFoo(): Foo = Foo(
  id = id,
  field1 = field1,
  // For fields with a default it is advised to define the default in the companion object
  field2 = field2 ?: Foo.DEFAULT_FIELD2,
  // List fields need the below conversion
  listField = listField.filterNotNull() ?: listOf(),
)
```

##### Using the DSL

```kotlin
class FooRepository(private val dsl: DSLReadWriteContext) {

  // normal insert
  fun insertFoo(foo: Foo) {
    dsl.write
      .insertInto(FOO, FOO.ID, FOO.FIELD1, /*...*/)
      .values(1, "", /* ... */)
      .execute()
  }

  // Insert and return entity with generated fields like ID or default values
  fun insertFooReturning(foo: Foo): Foo =
    dsl.write
      .insertInto(FOO, FOO.FIELD1, /*...*/)
      .values("", /* ... */)
      .returningResult(FOO.ID, FOO.GENERATED_FIELD)
      .fetchOne()!!
      .let { (id, generatedField) ->
        foo.copy(id = id!!, generatedField = generatedField)
      }

  // Batch insert
  fun insertFooBatch(foos: List<Foo>) =
    foos
      .fold(dsl.write.insertInto(FOO, FOO.FIELD1)) { insert, foo ->
        insert.values(foo.field1)
      }
      .execute()

  // Batch insert and return entity with generated fields
  fun insertFooBatchReturning(foos: List<Foo>): List<Foo> =
    foos.fold(
      dsl.write.insertInto(FOO, FOO.FIELD1, /*...*/)
    ) { insert, failedInvoice ->
      insert.values("", /* ... */)
    }
      .returningResult(FOO.ID, FOO.GENERATED_FIELD)
      .fetch().let { result ->
        foos.mapIndexed { index, foo ->
          foo.copy(field1 = result.getValue(index, FOO.FIELD1), /*...*/)
        }
      }

  // Upsert
  fun upsertFoo(foo: Foo) {
    dsl.write
      .insertInto(FOO, FOO.ID, FOO.FIELD1, /*...*/)
      .values(1, "", /* ... */)
      .onDuplicateKeyUpdate()
      .set(FOO.FIELD)
      .execute()
  }
}
```

#### Update

If you want to complete upsert a record, see [Insert or Upsert](#insert-or-upsert) and use the upsert.
However, normally an update should only cover a few fields, and you should always prefer the DSL

```kotlin
class FooRepository(private val dsl: DSLReadWriteContext) {
  /**
   * Update field1 of a foo
   * @return True if a row was updated, false otherwise
   */
  fun updateFooField1(id: Int, field1: String): Boolean =
    dsl.write
      .update(FOO)
      .set(FOO.FIELD1, field1)
      .where(FOO.ID.eq(id))
      .execute()
      .let { updatedRows -> updatedRows > 0 }
}
```
