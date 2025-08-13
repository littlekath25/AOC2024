# Tracing

## Useful resources

- https://docs.datadoghq.com/tracing/trace_collection/automatic_instrumentation/dd_libraries/java/?tab=wget
- https://docs.datadoghq.com/tracing/trace_collection/compatibility/java/

## Known issues

- There is currently [an issue](https://github.com/DataDog/dd-trace-java/issues/7215) when using `coroutineScope` where all tracing information is lost. Use
  `coroutineScopeTracingPreserved` instead or use `preserveTracingContext` when third party code uses `coroutineScope`.

## Usage

### preserveTracingContext

Use case: Call some third party code which starts a new coroutine scope. For example a GRPC coroutine stub.

```kotlin
import com.flexport.customs.lib.tracing.preserveTracingContext

// Third party code:
suspend fun someThirdPartyFunction() = coroutineScope {
  launch {
    // Do thing 1
  }
  launch {
    // Do thing 2
  }
}

// Instead of this:
someThirdPartyFunction()
// New detached trace is created :(

// Do this:
preserveTracingContext {
    someThirdPartyFunction()
}
// We continue with the same trace :)
```

### coroutineScopeTracingPreserved

Use case: Start a new coroutine scope and preserve tracing context afterwards

```kotlin
import com.flexport.customs.lib.tracing.coroutineScopeTracingPreserved

// Instead of this:
coroutineScope {
  launch {
    // do thing 1
  }
  launch {
    // do thing 2
  }
}
// New detached trace is created :(

// Do this:
coroutineScopeTracingPreserved {
  launch {
    // do thing 1
  }
  launch {
    // do thing 2
  }
}
// We continue with the same trace :)
```

## Prod setup

See https://docs.datadoghq.com/agent/cluster_agent/admission_controller/, https://github.com/flexport/customs-common-chart and [/server/helm/customs-filing-service/values.yaml](/server/helm/customs-filing-service/values.yaml)

In short:
- The DD admission controller automatically injects the DD agent into your pod
- The version of the DD agent is configured in [/server/helm/customs-filing-service/values.yaml](/server/helm/customs-filing-service/values.yaml)

## Testing locally

### 1. Download the datadog java agent

To get the latest version:
```
wget -O dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'
```

To download a specific version visit the [dd-java-agent maven repository](https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/)

### 2. Add the java agent to your VM run options:

```
-javaagent:/path/to/dd-java-agent.jar
```

Also make sure to enable the experimental Kotlin coroutines instrumentation:

```
-Ddd.integration.kotlin_coroutine.experimental.enabled=true
```

### 3. Optional: Enable debug mode

To enable debug logging, add the following option to your VM run options:

```
-Ddd.trace.debug=true
```
