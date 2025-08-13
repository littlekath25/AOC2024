# Logging

This template uses [kotlin-logging](https://github.com/oshai/kotlin-logging) as a logging library,
backed by logback and [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder) to give JSON
logging output.

## Examples

##### Create a logger

```kotlin
val log = KotlinLogging.logger { }
```

##### Log a message

```kotlin
// When just logging a message, both styles are OK
log.info("My message")
log.info { "My message" }
```

With the braces style, you avoid evaluating the log message string if the log level is not
enabled, so prefer the braces style for levels that are usually not active in production,
such as `log.trace` or `log.debug`, especially if you are dynamically constructing the string.
See [Brace Ourselves](https://www.baeldung.com/kotlin/kotlin-logging-library#1-brace-ourselves)
for more details.

##### logging an exception and stacktrace

This will log the exception class name, the exception message, and the developer's message.
```kotlin
log.error(e) { "There was a problem" }
```

This will not log the exception class name, so please avoid it.
```kotlin
log.error(e.message)
```

##### Add data to a log message

You can add structured data to a log message. In production, this will be encoded in the json.

```kotlin
import com.flexport.customs.lib.logging.warn

data class Data(val foo: String, val bar: String)

log.warn(
  "data" to Data("a", "b"),
  "shipmentId" to 123,
  "nested" to mapOf("a" to mapOf("b" to "c"))
) {
  "Could not process data"
}
```

Will result in logging these fields, among others:

```json
{
  "message": "Could not process data",
  "data": {
    "foo": "a",
    "bar": "b"
  },
  "shipmentId": 123,
  "nested": {
    "a": {
      "b": "c"
    }
  }
}
```

If you are also logging an exception, add it as the first parameter:

```kotlin
log.error(e, "shipmentId" to 123) { "Problem when processing shipment" }
```

##### Add context logging

You can also add string values to the logging context.
These will always be appended to logs running within the context.

For non-async code, you can use `withLoggingContext`.
When calling suspend functions, you need to use `withLoggingContextAsync`, otherwise the context will not propagate
This mechanism is backed
by [<abbr title="Mapped Diagnostic Context">MDC</abbr>](https://www.baeldung.com/mdc-in-log4j-2-logback).

```kotlin
import com.flexport.customs.lib.logging.withLoggingContextAsync
import mu.withLoggingContext

suspend fun logSomething() {
  withLoggingContext("context2" to "bar") {
    log.info("Hello world")
  }
}

fun main() {
  runBlocking {
    withLoggingContextAsync("context1" to "foo") {
      logSomething()
    }
  }
}

```

Will result in logging these fields, among others:

```json
{
  "message": "Hello world",
  "context1": "foo",
  "context2": "bar"
}
```

## Testing logging

You can use `CaptureLogsExtension` to verify that something logs as intended.

```kotlin
package my.package

class MyTest {
  private val log = mu.KotlinLogging.logger {}

  // This will capture all logs of the "my.package.MyTest".
  // You can provide the class you want to capture logging from or a string
  // You can add multiple capturers for different loggers
  @RegisterExtension
  val capturedLogs = CaptureLogsExtension<MyTest>()

  fun verifyLogging() {
    withLoggingContext("foo" to "bar") {
      log.info("Hello world")
    }

    val messages = capturedLogs.get().map { it.formattedMessage }
    messages should containExactly("Hello world")

    capturedLogs.json()[0] shouldEqualSpecifiedJson """{
            "message": "Hello world",
            "foo": "bar"
        }"""
  }
}
```
