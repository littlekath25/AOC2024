# Switches

Switches are a feature-flag technology that can be used to conditionally turn on or off access to particular code paths or product features. Feature flag switches are managed as configuration through the core UI and do not require deployment, migrations, or backfills to change. You can create and manage switches at https://core.flexport.com/switches. For additional information, please refer to the [Confluence doc](http://go/switches-wiki)

### Configure switch TTL and fallback

You can tune switches using a `switches` configuration in your configuration file:

```yaml
switches:
  cache:
    defaultTimeToLive: 3m  # Defaults to 1m
  slugs:
    my_switch_slug:
      timeToLive: 5m       # Defaults to 1m
      fallback: true       # Defaults to false
      throwOnError: false  # Defaults to false
    another_switch_slug:
      timeToLive: 10m
```

If `throwOnError` equals to `true` and the switch value cannot be retrieved from the server, the switch check will throw. Also, in this case the `fallback` value will be ignored.

> Note that the specific slugs configuration is not required

### Application setup

In your Application `Configuration`, add an entry for the switches configuration:

```kotlin
data class Configuration(
  // End from customs-kotlin-service-full-template, you can add your app-specific config below
  val switches: SwitchesConfiguration
)
```

and register this in Koin in ConfigurationModule.kt:

```kotlin
single { config.switches }
```

Finally, add the `SwitchesModule` to your Koin initialization in your `Application.kt`:

```kotlin
private val templateModules: List<Module> = listOf(
    authModule,
    ...,
    switchesModule
)
```

### Usage

To start using the switches, inject the `Switches` object into your class and start using its `isActive` method:

```kotlin
class MyService(private val switches: Switches) {
    fun doSomething() {
        if (switches.isActive("my_switch_slug", "some_key")) {
            // do something
        } else {
            // do something else
        }
    }
}
```
