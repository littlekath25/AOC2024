# Koin: dependency injection

[Koin](https://insert-koin.io/docs/reference/koin-core/dsl) is a kotlin library we use for dependency injection.
Dependency Injection (DI) is a design pattern where components (functions or more commonly classes) are given their dependencies instead of creating them internally.

[Read more](https://martinfowler.com/articles/injection.html)

# Examples

Koin bundles definitions in [modules](https://insert-koin.io/docs/reference/koin-core/modules).
A module is a bundle of related functionality that describes how to instantiate classes.

## Define your classes in a module
Within a module you describe how to construct your classes:
```kotlin
val myAppModule = module {
  // declare definitions here as described below
}
```

### Scopes
#### Singleton

Most commonly you will want to define your dependency as a singleton (object that is persistent with the entire container lifetime and can't be dropped)

```kotlin
val userModule = module {
  single { UserProfileService() }
}
```

If your component depends on other definitions, use `get()``:

```kotlin
val userModule = module {
    single { UserProfileService(userProfileRepository = get()) }
}
```

`UserProfileService` then looks like:
```kotlin
class UserProfileService(private val userProfileRepository: UserProfileRepository) {
    // your code here
}
```

Very commonly your definitions will have no dependencies or depend on other Koin dependencies, in which case you can use `singleOf` as a shortcut.
Also, often you will have a separate implementation and interface, in which case you need to `bind` the implementation to the interface:
```kotlin
val userModule = module {
    singleOf(::UserProfileServiceImpl) {
        bind<UserProfileService>
      }
    singleOf(::UserProfileController)
}
```

### Inject multiple implementations of the same interface

In case you have several implementations of the same interface, we may want to inject a particular implementation of the interface, in which case we can use `named`:

```kotlin
val kafkaProducersModule = module {
    single(named("customs-entry-changed-producer")) { CustomsEntryChangedProducer() }.bind<KafkaProducer>()
    single(named("task-changed-producer")) { TaskChangedProducer() }.bind<KafkaProducer>()

    // Equivalent singleOf syntax
    singleOf(::CustomsEntryChangedProducer) {
        named("customs-entry-changed-producer")
        bind<KafkaProducer>()
    }
    singleOf(::TaskChangedProducer) {
        named("task-changed-producer")
        bind<KafkaProducer>()
    }
}
```

And then resolve the dependency with `get(named("..."))`.

Once you do this you can get a `List` of all thing implementing the interface with getAll():
```kotlin
val kafkaModule = module {
  single { KafkaConsumerManager(conf = get(), messageConsumers = getAll()) }.onClose { it?.close() }
}
```

It is also possible to get injections based on the name individually, but this should be rare.

[See the Koin docs](https://insert-koin.io/docs/reference/koin-core/definitions/#definition-naming--default-bindings).

### Factory: new instance for every injection
The same registration and binding principles apply here as the ones described above. The only difference from `single` is that
koin creates new instance of the definition every time we ask for it. It is short-lived and cannot be shared among definitions.
Note that immutable classes are preferred, so you will usually want singletons instead.

```kotlin
factory { UserProfileService(userProfileRepository = get()) }
```

or
```kotlin
factoryOf(::UserProfileService)
```

#### Scoped

A scoped definition in Koin is used to provide instances with a lifespan that is bound to a given scope.
When you define an instance as scoped, it will live as long as its associated scope is alive. Once the scope is closed, the instance is dropped.
Scoped definitions are very uncommon and irrelevant for our usecase, so the document does not describe this in great detail.
[See this](https://insert-koin.io/docs/reference/koin-core/scopes) to know more.


## Nested modules
Note that modules can include modules:
```kotlin
val appModule = module {
  include(anotherModule)
}
```

This allows you to keep the code structured.

Koin app is then started as follows (the snippet is taken from [Application.kt](https://github.com/flexport/customs-kotlin-service-full-template/blob/main/customs-product-service/server/src/main/kotlin/com/flexport/customsproductservice/Application.kt#L33), at the injection root):

```kotlin
fun setupKoinApplication(): KoinApplication = startKoin {
  modules(
    configurationModule,
    databaseModule,
    authModule,
    graphqlModule,
    httpModule,
    grpcModule,
    kafkaModule,
    metricsModule,
  )
}
```

To add a new module to the application, follow the steps:
- Since module is supposed to bundle some related pieces of logic together, it is likely that you have a directory containing these pieces. In that folder, create a `YourModule.kt` file and define a module:
```kotlin
val yourModule = module {

}
```
- Import it in the injection root file (Application.kt) and add it to `setupKoinApplication` function shown above
- Use it in any of your classes registered within the modules listed under `setupKoinApplication`:
```kotlin
class Foo(private val myService: MyService) {
    // your code
}
```

## Testing with koin
You can both use existing modules in Koin to test the things in them, and define new modules inside your test to for example mock dependencies of the thing you are testing or create smaller modules specifically for your test.

You can enable Koin in your Junit5 test and create modules as follows:

```kotlin
  @JvmField
@RegisterExtension
val koinTestExtension = KoinTestExtension.create {
  modules(
    configurationModule,
    databaseModule,
    module { singleOf(::UserRepository) },
  )
}
```

Where `databaseModule` and `configurationModule` are modules we defined in the app, and `module { singleOf(::UserRepository) }` is a definition we are making available in this particular test.

In our test class, we can then get definitions from the modules we registered. For example, `databaseModule` defines `DSLReadWriteContext`, we can inject it by:

```kotlin
private val dslReadWriteContext: DSLReadWriteContext by inject()
```

`dslReadWriteContext` is then available to the whole test class.
