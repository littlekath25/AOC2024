# CronJob

Our backgroundjob library can be used for setting up simple (Unique over multiple pods) CronJobs.

Note that if a pods restarts at scheduling time a job might be skipped.

## Setting Up
First we need to create a Job.

```kotlin
class BusyJob(override val schedule: String) : BackgroundCronJob {
    override suspend fun run() {
        println("Hello World")
    }
}

```

The last thing we need to do is add the job to a koin module to it will be available to the CronScheduler.
Add a `cron/CronJobsModule.kt` to the server and add it to your `Application.kt`:


```kotlin
 single<RefreshJob>(named("BusyJob")) { BusyJob("0,30 * * * *")  }.bind<BackgroundCronJob>()

```

For the format of the schedule see these docs:  https://docs.inmo.dev/krontab/describing/string-format.html of the
Krontab lib. When running your application you should see Hello World every 30 seconds in the logs


## Creating Unique Jobs

When having multiple instances of your application running, sometimes you want only a single instance to do a CronJob
(e.g. to decrease the load on an API). For this you can use the `UniqueBackgroundCronJob` interface which uses the
[Distributed Lock](./distributed-lock.md) library under the hood. Follow that guide first.

Afterwards a minimal implementation looks like this.

```kotlin

class MyUniqueJob(
    val lockProvider: LockProvider,
    override val schedule: String
) : UniqueBackgroundCronJob {
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

To initialize the Job add the following to `cron/CronModule.kt`

```kotlin
    single(named(UniqueJob.name)) { UniqueJob(get(), "0,30 * * * *") }.bind<BackgroundCronJob>()

```


