# Metrics

This template uses [micrometer](https://github.com/micrometer-metrics/micrometer) as a observability facade library. So far we provide an example on how to configure the [DatadogMeterRegistry](../lib/metrics/src/main/kotlin/com/flexport/customs/lib/metrics/DatadogMeterRegistry.kt).

The metrics are disabled by default in local environment.

## Examples

##### Create a extention function to update the event counter by one

```kotlin
// In a file named "Metrics.kt" or similar in the relevant package for the metric
// or as a private function in the relevant file
package com.flexport.customsproductservice.yourservice

import com.flexport.customs.lib.metrics.DatadogMeterRegistry
import io.micrometer.core.instrument.Tag

fun DatadogMeterRegistry.incrementTestEvent(eventType: String) {
    counter("test_event.counter", listOf(Tag.of("event_type", eventType))).increment()
}
```
