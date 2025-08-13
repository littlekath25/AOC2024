# Integration Testing via JUnit

This package provides the infrastructure to run integration tests for the service in the JUnit test suite. These tests are run in their own task under `gradle :server:junitIntegrationTest` and are excluded from the general `gradle :test` task.

## Integration Testing Setup

All the integration tests in this package run against a single instance of the service which is specified under the `IntegrationTestServer` object. The server exposes its Koin instance as well as additional utilities to interact with the service during testing. The server is started before the first test are run and is shut down after the last test is completed. The startup and teardown process is managed by the `JUnitIntegrationTestExtension` which internally tracks the number of running tests and manages the server lifecycle using an `AtomicInteger`, this way we avoid having to set up and tear down the application between each test, which is a very time consuming process.

The individual tests are all run in parallel and examine the state of the objects that they created through either the exposed public APIs of the service or by interacting with the database directly in a whitebox manner. When possible, using the public APIs is strongly preferred, but it is allowed to inspect the database directly if using the APIs incurs particular additional complexity.

However the fact that we run all our tests in parallel against a single application instance adds a few restrictions to the things that we can do with these tests:

1. Restrictions on error cases: Since the tests are run against a single application, we cannot have tests that verify blocking or destructive failure cases. For example we cannot have a test that verifies that a consumer blocks when provided with a poison pill message - the test will block that consumer and all subsequent tests will fail.
2. Cascading failures: Due to the above, it is possible that a single test failure will cascade to multiple tests, making it harder to verify which test actually causes the failures.
3. Database state: Since the tests are run in parallel, we cannot guarantee that the database state is clean between tests. This means that tests should be written in a way that they can run in any order and not depend on the state of the database from previous tests. For example you can reference the existing ExecutionOrderEvent tests, each publishes an execution order created event, holds the shipment FID for it and then examines the Customs Filing Order and Escalations that are associated with that shipment FID. This way we can run the tests in any order, and they will not interfere with each other.

## Integration Test Setup

The JUnitIntegrationTest class is the location of all integration tests that are written with this paradigm. The class sets up the application modules via Koin and starts the application before running any of the tests. In addition, the class provides some additional testing utilities via the `IntegrationTestHooks` object that is passed into each test.

Currently, the `IntegrationTestHooks` object provides the following utilities:

1. The full Koin context of the application. This can be used to get specific services or components and is necessary if a database connection is required.
2. All the messages that have been produced to any Kafka topic that the application interacts with (Defined as any topic that is present in a Producer or Consumer configuration) via the `topicMessages` object. This can be used to verify that the application is producing the correct messages to Kafka.

### Writing a New Test

To write a new integration test, create a new test class within this package that contains your testing functionality and extending `KoinTest`. Your test class should be annotated witht he following annotations:

- `@ExtendWith(JUnitIntegrationTestExtension::class)`: This annotation tells JUnit to use the `JUnitIntegrationTestExtension` to manage the lifecycle of the application server.
- `@Tag("integration")`: This annotation tells JUnit that this test is an integration test and should be run as part of the `gradle :server:junitIntegrationTest` task.
- `@Execution(ExecutionMode.CONCURRENT)`: This annotation tells JUnit to run the tests in parallel. This is necessary to ensure that the tests run quickly and do not block each other.
