# Configuration management

## Getting Started - The `Configuration` data class

You can define configuration options for your application via the `Configuration.kt` data class. Here, you can define
both plain fields and nested configuration for different components via sub-configurations.

<details>
  <summary>Example</summary>

```kotlin
// Configuration.kt
data class Configuration(
  val appEnvironment: AppEnvironment = ConfigLoader.appEnvironment(),
  val welcomeMessage: String, // Plain field in parent configuration
  val http: HttpConfiguration, // Nested configuration for a different module
)

// HttpConfiguration.kt
data class HttpConfiguration(
  val port: Int,
  val gracePeriod: Duration,
  val timeout: Duration,
  val enabled: Boolean,
  val maxHeaderSize: Int,
)
```

</details>

Configuration is automatically loaded from `yaml` files in the classpath using a tool called Hoplite and is injected
into Koin via the ConfigurationModule.

Define configuration by creating `yaml` files in the `src/main/resources` folder, the following files are loaded:

- `configuration.yaml` this file defines default configuration and is used in all environments.
- `configuration-ENVIRONMENT.yaml` where ENVIRONMENT may be `local`, `staging` or `production`, these file define
  environment specific configuration and take precedence over the basic `configuration.yaml` file.

You can set configuration values as:
* A static value: `field: value`
* Loaded from an environment variable: `field: ${VARIABLE_NAME}`
* Loaded from an environment variable with a default: `field: ${VARIABLE_NAME:-default_value}`
* Loaded from an AWS Secret Manager secret: `field: awssm://secret_arn`
* Loaded from an AWS Secret Manager key in a key-value pair secret: `field: awssm://secret_arn[key]`

<details>
  <summary>Example YAML</summary>

```yaml
---
# configuration.yaml
welcomeMessage: "Hello world!"

http:
  # Flat values
  enabled: true
  maxHeaderSize: 16384
  # Reference to an environment variable (Without a default value, will crash if the variable is not present)
  gracePeriod: "${HTTP_GRACE_PERIOD}" # Environment variable without default
  # Reference to an environment variable with a default value
  timeout: "${HTTP_TIMEOUT:-500ms}"
  # Reference to AWS Secrets Manager secret (Note that the account arn will be different in staging and production)
  port: awssm://arn:aws:secretsmanager:us-east-1:740599847676:secret:app/staging/core_staging/customs_product_service/HTTP_PORT
  # Reference to AWS Secrets Manager key-value secret with a key
  hostAddress: awssm://arn:aws:secretsmanager:us-east-1:740599847676:secret:app/staging/core_staging/customs_product_service/HTTP_CONFIG[HOST_ADDRESS]
```

</details>

## Using Configuration

To use configuration in your application, simply inject it via Koin:

1. If it's a new sub-configuration, create the data class (for example `HttpConfiguration.kt`)
2. And add it to the `ConfigurationModule.kt`
3. Then you can use it in your class constructor and inject it in your Koin module

```kotlin
// ConfigurationModule.kt
val configurationModule = module {
  val config: Configuration = ConfigLoader.loadConfiguration()
  single { config }
  // Add your new sub-configuration in a single {} block so Koin can resolve it
  single { config.http }
}

// HttpServer.kt
class HttpServer(
  private val config: HttpConfiguration,
  // ...
) {
  // Use configuration here
}

// HttpModule.kt
val httpModule = module {
  single {
    HttpServer(
      // Inject the configuration via Koin
      config = get(),
      // ...
    )
  }
}
```

## Loading Configuration in Tests

To load configuration via Hoplite in tests, define it under `/test/resources/configuration-test.yaml`, you can define a
general configuration for your tests in this file, and it will be automatically loaded if you add a ConfigurationModule
to your tests.

This is the configuration that is loaded when using the Koin test extension:

```kotlin
@JvmField
@RegisterExtension
val koinTestExtension = KoinTestExtension.create {
    modules(
        configurationModule,
        httpModule,
    )
}

// This will automatically use the configuration from configuration-test.yaml
private val httpServer: HttpServer by inject()
// You can also directly inject the configuration
private val httpConfiguration: HttpConfiguration by inject()
```

## Mocking Configuration

If you want to define a different configuration for some of your tests, you can create a configuration object within
your tests and pass it to Koin or directly to the class under test if you're not using Koin in your test.

```kotlin
private val httpConfiguration = HttpConfiguration(
   port = 1234,
   // ...
)

@JvmField
// ...
module { single { httpConfiguration } }
@RegisterExtension
val koinTestExtension = KoinTestExtension.create {
    modules(
        module {
               single {
                    HttpConfiguration(
                      port=1234,
                        //...
                )
               }
        },
        httpModule,
    )
}
```

