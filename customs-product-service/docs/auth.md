# Authorization

Authorization is done by checking if the actor (user/service) has a specific Permission.

## What is a Permission

In the IAM's literature, each actor has a set of "Role"s.
Each Role by itself is made up of one or more "Policy".
A Policy is a set of "Operation"s.
An Operation represents an action happening in the system.
When a user possesses a role with Operation `foo`, the user has the "Permission" for doing `foo` or, shortly put, the
user has Permission `foo`.

A permission can be parameterized or non-parameterized.
Non-parameterized permissions only authorize against an action.
Parameterized permissions authorize against the data being acted on too.

## Non-Parameterized Permissions

### Creating Non-Parameterized Permissions

After reviewing the guidelines in [GraphQL Authorization Permissions](https://github.com/flexport/customs-adr/blob/main/decisions/accepted/0008-graphql-authorization-permissions.md),
you can follow this procedure for creating non-parameterized permissions:

1. Create an Operation representing the action you want to authorize
   in [CustomsOperation.java](https://github.com/flexport/flexport/blob/master/authorization/src/main/java/com/flexport/authorization/operations/CustomsOperation.java)
   like this:
   `YOUR_OPERATION_IN_CAPITAL_SNAKE_CASE(null, "your_permission_in_regular_snake_case")`
2. Add your operation to a role
   in [CustomsRolesConfiguration.java](https://github.com/flexport/flexport/blob/master/authorization/src/main/java/com/flexport/authorization/configuration/CustomsRolesConfiguration.java).
   The operation should be put inside a new or existing `Policy.forNonObjectLevel`.
3. Whenever you want to use the permission, you should actually use the "Permission Slug". For any operation `foo` added
   with the previous steps, the slug would be `customs:foo` (`customs` being the service name inside
   CustomsOperation.java)

As an example, in [https://github.com/flexport/flexport/commit/893c3bbb864b7edd00ec141b0bf796fa7d4f56a3], a few
operations are added to the role "Customs Broker" and the roles above it[^1]. As a usage example, you can see [here](https://github.com/flexport/customs-access-manager/blob/main/server/src/main/kotlin/com/flexport/customsaccessmanager/graphql/queries/CustomsRolesQuery.kt#L31) and [here](https://github.com/flexport/customs-access-manager/blob/main/server/src/main/kotlin/com/flexport/customsaccessmanager/auth/permissions/Permission.kt#L5).

[^1]: In CustomsRolesConfiguration.java, code is structured in a way to make the roles hierarchical, meaning that any
operation added to a role would also be added to the roles higher in the hierarchy. Be sure that you do not violate this
attribute.

If you want more details into the process, you can refer to the "[Adding a Permission to a User](https://flexport.atlassian.net/wiki/spaces/CU/pages/2827616273/Authorization+Notebook#Adding-a-Permission-to-a-User)" section of the Authorization Notebook.

### Non-Parameterized Authorization in GraphQL

Flexport-issued JWT contains a list of claims which include all the non-parameterized permissions an actor possesses.
Using the [Flexport auth library](https://github.com/flexport/flexport/tree/master/common/jvm/lib/auth), the
token is decoded to access the claims[^2].

[^2]: Follow
this [confluence doc](https://flexport.atlassian.net/wiki/spaces/CU/pages/2658894347/Customs+Add+authentication+to+your+public+API+routes+in+a+service)
on how to authenticate your routes with an exchanged IAM token and test them out locally.

Using [directives](https://opensource.expediagroup.com/graphql-kotlin/docs/schema-generator/customizing-schemas/directives/),
the `Authorize` annotation is designed to be used on mutations, queries and fields to authorize them against a
non-parametric permission.
The directive also commits the permissions in the schema, making it easier for the federation to know what permissions
are needed in a service.

```kotlin
import com.flexport.customsproductservice.graphql.directives.Authorize
import graphql.schema.DataFetchingEnvironment
import com.expediagroup.graphql.server.operations.Mutation

class MyMutation : Mutation {

  @Authorize("my_permission")
  suspend fun myMutation(dataFetchingEnvironment: DataFetchingEnvironment) {
    // Do stuff..
  }
}
```

#### Local environment

Authentication for graphql is disabled by default for local environments, to prevent JWT generation of being an
obstacle during development.
You can test your queries that do require authentication by setting the `graphql.authorization.enabled` config
to `true` in `configuration-local.yaml` and following the instructions above to acquire a token.

#### Demo

Inspired by the [Bridge of Death](https://www.youtube.com/watch?v=VvSO5KEnaVE),
and [Killer Rabbit](https://www.youtube.com/watch?v=qaCJtXX87aU) scenes,
we can have the following types, query, and mutation:

```kotlin
data class QuestionsThree(
  val knight: String,
  val question: String,
  @Authorize("can_answer_questions_3")
  val answer: String?,
)

data class RabbitOfCaerbannog(
  val dead: Boolean,
)

class RabbitOfCaerbannogMutation {

  @Authorize("has_holy_hand_grenade")
  fun destroy(): RabbitOfCaerbannog {
    return RabbitOfCaerbannog(dead = true)
  }
}

class BridgeKeeperQuery : Query {

  @Authorize("cross_bridge")
  fun askQuestionsThree(): List<QuestionsThree> {
    return listOf(
      QuestionsThree("Sir Robin", "What is your name?", "Sir Robin of Camelot."),
      QuestionsThree("Sir Robin", "What is your quest?", "To seek the Holy Grail."),
      QuestionsThree("Sir Robin", "What is the capital of Assyria?", "I don't know that!"),
    )
  }
}
```

As can be seen, we put the `Authorize` directive on top of the `askQuestionsThree` query, `destroy` mutation,
and the `answer` field, each checking against different permissions.
After generating the SDL, the following schema is produced:

- In the mutation, the `has_holy_hand_grenade` will be checked and error will be returned if the user does not have the
  permission.
- In the query, both `cross_bridge` and `answer_questions_3` permissions will be checked.
- If the field `answer` is not included when doing the query, only the `cross_bridge` permission is checked.
- If the field `answer` is nullable like defined above (`String?`),
  in case the user does not have the permission needed for the field,
  the response would be partial (meaning that it would contain both the data with `answer=null` and the errors).

### Graphql Non-Parameterized Authorization Implementation in Monorepo
In Monorepo, authorization happens differently depending on where the request is coming from.
`accessible_to` is the directive used for authorizing a Graphql entity/field.
This directive takes as argument, the "App Name", and the non-parameterized permissions list:

```ruby
class ClientType < ::GqlTypes::BaseObject
OBJECT_T = ::Client

    accessible_to ::AppNameService::AppName::Core, "permission_1", "permission_2"
    accessible_to ::AppNameService::AppName::Client, "permission_1", "permission_4"
    accessible_to ::AppNameService::AppName::General, "permission_3", "permission_5"
...
```


Consider the `CustomsFilingOrder` entity below that has the `client` field referencing `Client` type through federation in Customs V2:

```graphql
type Client @extends @key(fields: "fid") {
  fid: String! @external
}

type CustomsFilingOrder {
  ...
  client: Client!
  ...
}
```

For Customs V2 the "App Name" is `::AppNameService::AppName::General`.
This means when user wants to reference `client` through `CustomsFilingOrder` in Customs V2,
They need to have either `permission_3` or `permission_5`.
**If the user doesn't have the necessary permissions, the Graphql returns `null` for the Monorepo federated field**.
This could get problematic if like the definition above (`client: Client!`),
the field is non-nullable and result in getting a Graphql error like
**"Cannot return null for non-nullable field CustomsFilingOrder.client"**.

For avoiding any unwanted issues, check the Monorepo types you are referencing and make sure that your users have at least one of the required permission.
