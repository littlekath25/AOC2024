# GraphQL

In this guide we go over how to define GraphQL queries and mutations as well as GraphQL federation and integration tests for GraphQL queries.

## GraphQL queries and mutations

To set up simple GQL queries and mutations, we need to follow a couple of steps.
The steps are similar for queries and mutations, so we will only describe queries in detail.

1. Create a file under `/server/src/main/kotlin/com/flexport/customsproductservice/graphql/queries/`, e.g. `MyQueries.kt`
2. Define a `MyQueries` class with a function for the query:
```kotlin
import com.expediagroup.graphql.server.operations.Query

class MyQueries : Query {
  fun foo(count: Int): String {
    return "$count bars"
  }
}
```
3. Register `MyQueries` within `QueryModules.kt` :

```kotlin
import com.expediagroup.graphql.server.operations.Query
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val queryModules = module {
    singleOf(::MyQueries) {
        named<MyQueries>()
        bind<Query>()
    }
}
```

4. Run `./gradlew server:graphqlGenerateSDL`. This will update your `server/schema.graphqls` with the new root query.

5. Restart your app: at step #4 we changed the code, so we need to recompile it
6. Open `http://localhost:8080/playground` and run the following query:

```graphql
query {
  foo(count: 5)
}
```

The output should be `"5 bars"`.

Note: If you're defining a mutation, use the same steps but in the mutations directory.

### Defining a custom input/return type

In order to return/accept an application-specific object type in your graphql query or mutation follow the steps:

1. Add a GraphQL [DTO](https://en.wikipedia.org/wiki/Data_transfer_object) class to `/server/src/main/kotlin/com/flexport/customsproductservice/graphql/types/`:

```kotlin
@GraphQLDescription("...")
data class GqlFoo(
  val bars: Int
)
```

2. Add conversion functions to the same file as extension methods:

```kotlin
import com.flexport.customsproductservice.app.entities.Foo as AppFoo

fun AppFoo.toGraphQL(): GqlFoo = GqlFoo(
  bars = bars
)

// If needed for gql input args:
fun GqlFoo.fromGraphQL(): AppFoo = AppFoo(
  bars = bars
)
```

3. Return the type from your query:

```kotlin
class MyQueries(private val fooService: FooService) : Query {
  fun lastFoo(): GqlFoo = fooService.getLastFoo().toGraphQL()
}
```

4. Or accept a type as an argument for your query/mutation and convert it to a representation that your app can understand:

```kotlin
class MyMutations(private val fooService: FooService) : Mutation {
  fun insertFoo(inputFoo: GqlFoo): GqlFoo {
    val foo = fooService.create(inputFoo.fromGraphQL())
    return foo.toGraphQL()
  }
}
```

### Defining a custom scalar type

Some Kotlin scalar types like `Int` and `String` you can already return.
At the moment of writing, there are still gaps in what we want to support by default, eventually common types like `Instant` and `UUID` should be supported out of the box.

Until the template is improved on this point, you can see an example of how to implement a scalar here:
https://github.com/flexport/customs-kotlin-service-full-template/pull/233

### Current User
If you need infos such as `fid` or `email` of the current user running the query/mutation, you can
use the `dataFetchingEnvironment.currentUser` extension function:

```kotlin
import com.expediagroup.graphql.server.operations.Query
import com.flexport.customsproductservice.graphql.auth.currentUser
import graphql.schema.DataFetchingEnvironment

class MyQueries : Query {
  fun whoAmI(dataFetchingEnvironment: DataFetchingEnvironment): String {
    val currentUser = dataFetchingEnvironment.currentUser
    return "Email: ${currentUser.email}, Fid: ${currentUser.fid}"
  }
}
```
## GraphQL Federation

GraphQL federation is a way to compose multiple GraphQL services into a single GraphQL API. It is a way to split a large schema into smaller, more manageable schemas. It is also a way to share data between services.

In this guide we will go over two scenarios:
1. Extending an existing federated GraphQL type with a new field
2. Referencing a federated GraphQL type defined in another service

*Note*: When referencing a federated type in Monorepo, beware of the app permissions
(see [auth.md](./auth.md#graphql-non-parameterized-authorization-implementation-in-monorepo)).

### 1. Extending existing federated GraphQL type

In this example we will extend the `CustomsEntry` type from the `core` service with `tasks` field from the our NGIF service.

#### Prerequisites

Make sure that the type you're extending is an apollo federation entity, meaning:
- it has a `@key` directive defining its primary key(s)
- it has a resolver in one of the services that loads the entity by its primary key(s)

This is required for federation to work.

In case of `CustomsEntry` type it looks the following way:

```ruby
# customs_entry_type.rb

key fields: :fid

field :fid, ::String, null: false

def self.resolve_reference(reference, context)
  ::Loaders::Fid::ActiveRecord.for(
    ::CustomsDeclarationUs::CustomsEntry,
    context[:current_user],
    current_app_name: context[:current_app_name],
  ).load(reference[:fid])
end
```

Which results in the following SDL:

```graphql
# core.graphqls

type CustomsEntry @key(fields: "fid") {
  fid: String!
  # ...
}
```

#### Extending the type

Let's add tasks field to the `CustomsEntry` type, which will return all tasks associated with the customs entry.

First, we need to define a new type for the task:

```kotlin
// graphql/types/CustomsEntryTask.kt

data class CustomsEntryTask(
    val displayName: String
)
```

Second, let's extend the `CustomsEntry` type with the new field:

```kotlin
// graphql/types/CustomsEntry.kt

@KeyDirective(fields = FieldSet("fid"))
@ExtendsDirective
data class CustomsEntry(
    @ExternalDirective val fid: String,
) {
    fun tasks(dataFetchingEnvironment: DataFetchingEnvironment): CompletableFuture<List<CustomsEntryTask>?> {
        // your own implementation, preferably using DataLoader, for example
        return dataFetchingEnvironment.getValueFromDataLoader("TasksByCustomsEntryFidDataLoader", fid)
    }
}
```

Note that:
- the key directive is the same as in the original type
- we're using `@ExternalDirective` annotation for the `fid` field, which tells federation that this field is not defined in the current service, but in another service.

After running the Gradle `generateSDL` task, the following SDL will be generated:

```graphql
# schema.graphqls

type CustomsEntry @extends @key(fields : "fid") {
  fid: String! @external
  tasks: [CustomsEntryTask!]
}
```

**Important:** avoid non-null field types, especially on subgraph boundaries, because if one subgraph is temporarily unavailable, the entire query will fail. Note how `tasks` field is defined as nullable in the example above.

More context on this can be found in [GraphQL Schema Design Best Practices](https://flexport.atlassian.net/wiki/spaces/EN/pages/2680586251/GraphQL+Schema+Design#Avoid-non-null-fields).


### 2. Referencing federated GraphQL type from another service

In this example we will make our local `CustomsEntryTask` type have a field `updatedBy` that references `User` type from the `auth` service.

#### Prerequisites

Make sure that the type you're referencing is an apollo federation entity. To verify this you can use the criteria defined in the [Extending existing federated GraphQL type](#extending-existing-federated-graphql-type) section.

#### Referencing the type

Let's add `updatedBy` field to the `CustomsEntryTask` type, which will return the user that last updated the task.

```kotlin
// graphql/types/CustomsEntryTask.kt

data class CustomsEntryTask(
    // ...
    val updatedBy: User?
)
```

Then define reference `User` type:

```kotlin
// graphql/types/User.kt

@KeyDirective(fields = FieldSet("fid"))
@ExtendsDirective
data class User(@ExternalDirective val fid: String)
```

Note that it contains only key field `fid` and no other fields.

Then whenever we creating a `CustomsEntryTask` object, we need to set the `updatedBy` field to an instance of our new `User` reference type:

```kotlin
CustomsEntryTask(
    // ...
    updatedBy = task.updatedByFid?.let { User(it) },
)
```

After running the Gradle `generateSDL` task, the following SDL will be generated:

```graphql
# schema.graphqls

type CustomsEntryTask {
  # ...
  updatedBy: User
}

type User @extends @key(fields : "fid") {
  fid: String! @external
}
```

**Important:** avoid non-null field types, especially on subgraph boundaries, because if one subgraph is temporarily unavailable, the entire query will fail. Note how `updatedBy` field is defined as nullable in the example above.

### 3. Creating a new root federated type in NGIF

In this example we will create a new federated type in NGIF and add the necessary components to make a type that's
defined in an NGIF subgraph federated, thus allowing other subgraphs to nest it and extend it just as we nested
and extended core types in the previous examples.

The first part to allow this to happen is to annotate the type with the `@KeyDirective` annotations, providing the key
for our entity.

```kotlin
@KeyDirective(FieldSet("fid"))
data class CustomsUsIsf(
  val fid: Fid,
  val headerTransactionNumber: String,
  // ...
)
```

When we do this and regenerate the GraphQL schema we will see that the `@key` directive is added to the type in the
subgraph and more importantly our type is added to the `_Entity` union which represents all federated types in our
subgraph.

With just this step we're able to reference our type from other subgraphs, but we're not able to actually resolve it,
since we haven't provided any logic regarding how to actually query the type (Even if we already have queries that load
it GraphQL does not know how to use them yet).

To do this we need to provide a resolver for our type. This is done by creating a class that implements the
`FederatedTypeResolver<*>` interface. This can use service calls directly, but it's recommended to use data loaders
instead to avoid N+1 queries. This can be done by following the pattern below:

```kotlin
class CustomsUsIsfResolver : FederatedTypeResolver<CustomsUsIsf> {
    override val typeName: String = "CustomsUsIsf"

    override suspend fun resolve(
        environment: DataFetchingEnvironment,
        representations: List<Map<String, Any>>,
    ): List<CustomsUsIsf?> {
        val ids = representations.map { Fid.parse(it["fid"] as String) }

        // We need to call dispatch to fire off the data loader cache for execution, otherwise the load will never
        // actually happen and calling .join() will just hand forever. There is better support for this pattern in
        // expedia GraphQL 7.0, so we should probably upgrade soon-ish.
        return environment.getDataLoader<Fid, CustomsUsIsf?>("CustomsUsIsfLoader").run {
            val entities = loadMany(ids)
            dispatch()
            entities.join()
        }
    }
}
```

In order to actually use the resolver, we need to add it to the schema generator hooks in addition, but the hooks are
not yet configured in the template. If you use custom coercings then you probably already have the hooks configured,
otherwise you can see a sample implementation in customs-log-aggregator [here](https://github.com/flexport/customs-log-aggregator/pull/47/files#diff-fb0219d8aaa2994b0de863d0692d960d28c1471b858a9f6a17942bb6341d1c53).
Once the resolver is added there then the type will be fully federated and can be used & resolved from other subgraphs
to be nested or extended.

## GraphQL integration testing

GraphQL integration tests are set up using a Ktor test application. For convenience this is implemented in the
`graphQLTest` extension function on `KoinTest`. This function should be called with a block, which is used as an
extension on the `GraphqlTestClient`.

To test a query, execute it within the block using `GraphQLTestClient.runQuery`, or execute a query from a fixture using
`GraphQLTestClient.runFixtureQuery`. The infix function `shouldBeGraphQLResponse` should be used to assert a correct
response.

Example with query:
```kotlin
fun myTest = graphQLTest {
  runQuery("""{__typename""") shouldBeGraphQLResponse """{"__typename": "Query"}"""
}
```

Example using fixture:
```kotlin
fun myTest = graphQLTest {
  val fixtureName = "myFixtureName"
  runFixtureQuery(fixtureName) shouldBeGraphQLResponse getExpectationFixture(fixtureName)
}
```

## Custom GraphQL Exceptions
By default, any exception thrown in a GraphQL query or mutation is picked up by the library and serialized in the
following way:

```json
{
    "data": ...,
    "errors": [
        {
            "message": "Exception while fetching data (/$QUERY_NAME$) : $YOUR_EXCEPTION_MSG$",
            "locations": [
                {
                    "line": 2,
                    "column": 5
                }
            ],
            "path": [
                "$QUERY_NAME$"
            ]
        }
    ]
}
```

In other words, the exception message is wrapped in a generic message and any other fields are omitted.

If you want more custom behavior, e.g., you want to add a status code to your exception, you have to extend the
`GraphqlErrorException` class. For example:

```kotlin
data class GraphQLWithCodeException(override val message: String?, val errorCode: String) :
    GraphqlErrorException(newErrorException().message(message)) {
    override fun getExtensions(): MutableMap<String, Any> {
        return mapOf("code" to errorCode).toMutableMap()
    }
}
```

Now when this exception is thrown, it is serialized in the following way:
```json
{
    "data": ...,
    "errors": [
        {
            "message": "Exception while fetching data (/$QUERY_NAME$) : $YOUR_EXCEPTION_MSG$",
            "locations": [
                {
                    "line": 2,
                    "column": 5
                }
            ],
            "path": [
                "$QUERY_NAME$"
            ],
            "extensions": {
                "code": "$YOUR_ERROR_CODE$"
            }
        }
    ]
}
```
Notice the `code` field in the `extensions` object. Similarly, you can add any other fields to the `extensions` object
or override any other methods in the `GraphqlErrorException` class to customize the serialization of your exception.
