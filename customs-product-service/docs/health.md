# Health
The application has two health probes configured in kubernetes; `liveness` and `readiness`. The application is only considered healthy, if both probes are satisfied.
Our application exposes two endpoints, one for each probe at `/health/liveness` and `/health/readiness`. They work by running the set of `HealthChecker`s, that are configured for the respective probe.

If any of the health checks for a fails, the endpoint will respond with a `ServiceUnavailble` error and the probe will fail. The helm template contains [configuration](https://github.com/flexport/customs-common-chart/blob/de144b986ae6e9c949b6b241c1dbcda7871baa47/helm/customs-common-chart/templates/deployment.yaml#L170-L183) that describes how often the probes are validated and how long they can fail for before action is taken.

### Liveness vs Readiness
If the `liveness` probe fails for too long, Kubernetes will automatically restart the application instance. This is used for checking the health of critical components.
For example Database connectivity.

If the `readiness` probe fails for too long, Kubernetes will stop routing traffic to the application. Applications can be in this state indefinitely.

More information on k8s `liveness` vs `readiness` can be found [here](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/),

### Configuration example
The names are matched against all the [HealthChecker.name](../lib/core/src/main/kotlin/com/flexport/customs/lib/core/HealthChecker.kt) implementations that can be found through Koin.

```kotlin
class ExampleHealthChecker: HealthChecker {
  override val name: String = "exampleHealthChecker"

  override suspend fun checkHealth(): HealthResult {
    HealthResult(
      status = HealthStatus.Healthy,
      information = mapOf("additonalInformation" to "relevant data exposes in the JSON response"),
    )
  }
}
```

```yaml
# configuration.yaml
health:
  probes:
    liveness:
      - database
      - exampleHealthChecker
    readiness:
      - anotherExampleHealthChecker
```

### Health Checker
An [interface](../lib/core/src/main/kotlin/com/flexport/customs/lib/core/HealthChecker.kt) that can be implemented to validate the state of a certain part of our application. A good example is the [DatabaseHealthChecker](../server/src/main/kotlin/com/flexport/customsproductservice/database/DatabaseHealthChecker.kt) which validates that the application can communicate with the DB.

In order to enable your custom health check, register your `HealthChecker` as follows in a Koin module:
```kotlin
singleOf(::MyHealthChecker).bind<HealthChecker>()
```
and add it's `name` to the [configuration](https://github.com/flexport/customs-common-chart/blob/de144b986ae6e9c949b6b241c1dbcda7871baa47/helm/customs-common-chart/templates/deployment.yaml#L170-L183) under `liveness` or `readiness` probes.
