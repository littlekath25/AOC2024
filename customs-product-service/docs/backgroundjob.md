# BackgroundJob

Our backgroundjob library can be used for setting up BackgroundJobs that can also be utilizing distributed lock
in case you need it unique over multiple pods.


## Setting Up
First we need to create a Job.

```kotlin
class TestJob() : BackgroundJob {
    override suspend fun run() {
        println("Hello World")
    }
}
```

Then we need to add the job to a koin module, so it will be available to the `BackgroundJobRunner`.
Add a `backgroundjob/BackgroundJobRunnerModule.kt` to the server and add it to your `Application.kt`:


```kotlin
 single<TestJob>(named("TestJob")) { TestJob()  }.bind<BackgroundJob>()
```

`BackgroundJobRunner` runs in `GlobalScope` asynchronously, which means it will stay active for
the whole duration of the application's lifetime.

Additionally, `BackgroundJobRunner` will run each background job
in a single thread. Make sure that the background jobs you create are suitable for this
setup.

## Creating Unique Jobs

When having multiple instances of your application running, sometimes you want only a single instance to do a BackgroundJob
(e.g. to decrease the load on an API). For this you can use the `UniqueBackgroundJob` interface which uses the
[Distributed Lock](./distributed-lock.md) library under the hood. Follow that guide first.

Example implementation below:

```kotlin
class UniqueJob(
    val lockProvider: LockProvider,
) : UniqueBackgroundJob {
    companion object {
        const val name = MyUniqueJob::class.simpleName!!
    }
    override val name = Companion.name

    override val lock: Lock =
        DistributedLock(
            lockProvider,
            LockConfiguration(name),
        )
    override suspend fun runUnique() {
        println("HELLO WORLD")
    }
}
```

By default, the job will retry to acquire lock after 1 minute. You can adjust this value by overriding `retryAcquireLockInterval` in your job.

To initialize the Job, you will need to add it to `BackgroundJobRunner` (same as `BackgroundJob`).
