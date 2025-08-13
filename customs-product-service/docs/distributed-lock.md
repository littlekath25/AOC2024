# Setting up the Distributed Lock

This service templates include a distributed locking framework. For it to work we need to make sure that our Database
is set up correctly. First create a new [Flyway Migration](./database.md) with the following contents.

```postgresql

CREATE TABLE shedlock(name VARCHAR(64) NOT NULL, lock_until TIMESTAMP NOT NULL,
                      locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));

```

Make sure that you apply the migration & generate the JooQ types.

## Using Distributed Lock

Add `distributedLockModule` to the Koin initialization in your `Application.kt` afterwards you can create a lock in the following way

```kotlin
val lockProvider : LockProvider = get()

val lock: Lock =
        DistributedLock(
            lockProvider,
            LockConfiguration("lock_name", lockTimeout, LockHold),
        )

val result = lock.runWithLock {
    // do something which you want to do once
    return "FOO"
}
```
In the above example result will be "FOO" if the lock is not in use. Else it will be null. For testing you can use
`InMemoryLockProvider which simplifies test setup.

