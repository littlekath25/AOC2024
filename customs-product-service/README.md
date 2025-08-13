# customs-product-service

| Staging                                                                         | Prod                                                                        |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| [Staging deploy](https://argocd.flexport.io/applications/argocd/customs-product-service-stage) | [Prod deploy](https://argocd.flexport.io/applications/argocd/customs-product-service-prod) |

A service application that services product related data.

Is this the first time you're reading this after creating this service from a backstage template?
See what to do next in the [Setup](docs/setup.md) documentation!

## Development

### Common commands

| Operation                                                                                                  | Command                                           |
|------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| Full build                                                                                                 | `./gradlew build`                                 |
| Run local server                                                                                           | `./gradlew server:run`                            |
| Run server tests                                                                                           | `./gradlew server:test`                           |
| Run tests [matching a pattern](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering) | `./gradlew server:test --tests '*MyClassTest'`    |
| Apply formatter and linter                                                                                 | `./gradlew ktlintFormat`                          |
| Start local Kafka, Postgres, Mockserver.                                                                   | `docker compose --project-directory server up -d` |
| Truncate local DB and apply migrations                                                                     | `./scripts/generate-db-schema.sh`                 |
| Update `server/schema.graphqls`                                                                            | `./gradlew server:graphqlGenerateSDL`             |

### Local Environment

```shell
git config blame.ignoreRevsFile .git-blame-ignore-revs
./gradlew installGitHooks
# Next: run commands from the "Database" section
# You should now be able to do a full build
./gradlew build
```

##### AWS
To change AWS configuration, edit [terraform/local/main.tf](./terraform/local/main.tf)

To apply Terraform on Local Stack run `./scripts/tf-local-apply.sh`.

To interact with AWS install [awslocal](https://github.com/localstack/awscli-local) and check its usage guide.

##### Database

Below commands initializes a local database and generates Kotlin code representing the database.
*You will need to run them again after schema changes*.
Make sure you have [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed.

Running this will fix the error `FATAL: database "customs_product_service_develop" does not exist`

```shell
docker compose --project-directory server up -d
./scripts/generate-db-schema.sh
docker compose --project-directory server down
```

##### Graphql

While running the local server, you can access the server's GraphQL playground at http://localhost:8080/playground

## Editor setup

### IntelliJ

#### Linting


Advised settings:
* [Format Files on save](https://www.jetbrains.com/help/idea/reformat-and-rearrange-code.html#reformat-on-save) and
* [Optimize imports on save](https://www.jetbrains.com/help/idea/creating-and-optimizing-imports.html#optimize-on-save)
* Install git commit hook: `./gradlew installGitHooks`

#### JVM
Ensure that the project is configured to use Correto 21 as the JVM SDK. You can download the JDK
[here](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html) (For MacOS, download the `.pkg` file)
![IntelliJ SDK Config](docs/images/sdk-corretto-21.png)

Also make sure that the Gradle JVM version is set to the same SDK.
This is configured at: Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM.

### VS Code

[Install EditorConfig support](https://marketplace.visualstudio.com/items?itemName=EditorConfig.EditorConfig).

## FAQ

#### How should I do X?

See the [docs](docs/) folder:

* [Auth](docs/auth.md)
* [Configuration](docs/configuration.md)
* [Database](docs/database.md)
* [Dependency management](docs/dependency-management.md)
* [Deployment](docs/deployment.md)
* [GraphQL](docs/graphql.md).
* [gRPC and Protobuf](docs/grpc-and-protobuf.md)
* [Logging](docs/logging.md)
* [Kafka](docs/kafka.md)
* [Kafka-connect](docs/kafka-connect.md)
* [Koin: dependency injection](docs/koin-dependency-injection.md)
* [Setup for a new service](docs/setup.md)
* [Secret management](docs/secrets.md)
* [Slack Notification](docs/slack-notification.md)
* [Testing](docs/testing.md)
* [Sentry](docs/sentry.md)

#### How do I learn more about X?

See [Confluence Customs: NGIF/Kotlin](https://flexport.atlassian.net/wiki/spaces/CU/pages/2559180864/Customs+NGIF+Kotlin),
especially [Learning Materials / Kotlin 101](https://flexport.atlassian.net/wiki/spaces/CU/pages/2744090858/Learning+Materials+Kotlin+101).

#### How do I upgrade to the latest template?

See [Confluence: How to upgrade your NIS repo from upstream template](https://flexport.atlassian.net/wiki/spaces/CU/pages/2625176112/Customs+How+to+upgrade+your+NIS+repo+from+upstream+template).

#### How do I change something in the template?

Make the change in your application, and then open a PR on to upstream it.
See [Confluence: How to upstream changes from an app to the template](https://flexport.atlassian.net/wiki/spaces/CU/pages/2684289375/Customs+How+to+upstream+changes+from+an+app+to+the+template).

#### My question isn't listed here!

That's not a question, but feel free to ask anything on
Slack [#customs-ngif-template](https://flexport.slack.com/archives/C03QNNS448N)

