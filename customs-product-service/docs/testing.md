# Testing

For testing, we use the following libraries:

| Purpose                   | Library                                                                 |
|---------------------------|-------------------------------------------------------------------------|
| Testing framework         | [JUnit5](https://junit.org/junit5/)                                     |
| Assertions and matchers   | [Kotest matchers](https://kotest.io/docs/assertions/core-matchers.html) |
| Mocking application code  | [Mockk](https://mockk.io/)                                              |
| Mocking external services | [MockServer](https://www.mock-server.com/)                              |

Additionally, since we use Koin and dependency injection,
it can be useful to read the [Koin: dependency injection](koin-dependency-injection.md) docs to understand
how it works.

### Test Location
Tests should be placed in `/server/src/test/kotlin/` source folder (replace server with your project if it isn't server).
The test must be in the same package as the subject under test, and if what you are testing is in `<File>.kt`,
the test must be in `<File>Test.kt`. This will usually be `<ClassBeingTested>Test.kt`.
If you use IntelliJ, you can use the "Go to Test" action (Mac: `⇧⌘T`) to go to or create the test file.

For example, if you are testing
`/server/src/main/kotlin/com/flexport/customsproductservice/application/Calculator.kt`
the test  must be in
`/server/src/test/kotlin/com/flexport/customsproductservice/application/CalculatorTest.kt`.

### Test Structure

Within the test class, tests must be methods annotated with `@Test`.
Making sure you import `org.junit.jupiter.api.Test` (JUnit5) and not `org.junit.Test` (JUnit4) or `kotlin.test.Test`.

If you are testing multiple things for the same method or functionality, you can use `@Nested` to group them ,
but this is not required if there are no natural subgroups.

This means a test will look something like this:

```kotlin
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CalculatorTest {

  @Nested
  inner class SumTests {
    @Test
    fun `sums positive numbers`() {
        Calculator.sum(2, 2) shouldBe 4
    }

    @Test
    fun `sums negative numbers`() {
        Calculator.sum(-2, -2) shouldBe -4
    }
  }
}
```

## Assertions

We use [Kotest matchers](https://kotest.io/docs/assertions/core-matchers.html) for assertions.
It has a lot of matchers out of the box and integrates well with Kotlin.
Try to find the most specific matcher for your use case, as this improves test failure output quality and test readability.
Avoid creating a boolean output and asserting on that, as this will not show what is wrong very well.

#### Examples

```kotlin
@Test
fun `basic matchers`() {
  2 + 2 shouldBe 4
  1 shouldNotBe 2
  1 shouldBeLessThan 2
}

@Test
fun `exception matchers`() {
  shouldThrow<IllegalArgumentException> {
    Calculator.divide(1, 0)
  }

  val e = shouldThrow<IllegalArgumentException> {
    Calculator.square_root(-1)
  }
  e shouldHaveMessage "Cannot calculate sqrt(-1), imaginary numbers are not supported."
}

@Test
fun `collection matchers`() {
  listOf<Int>().shouldBeEmpty()
  listOf(1, 2, 3) shouldContain 2
  listOf(1, 2, 3).shouldContainExactly(1, 2, 3)
  listOf(1, 2, 3).shouldContainExactlyInAnyOrder(3, 2, 1)
}

@Test
fun `json matchers`() {
  output shouldEqualJson """{
    "field1": "value1",
    "field2": "value2",
  }"""

  // Ignore extra fields in output
  output shouldEqualSpecifiedJson """{
     "field1": "value1"
  }"""
}
```

#### Examples of what not to do

Do not use the JUnit assertions as they are not very expressive and do not give good test failure output.
```kotlin
fun `use JUnit assertions`() {
  assertTrue(Calculator.sum(2, 2) == 4)
}
```

Do not create a boolean expected result and assert on that, as this will not show what is wrong very well on test failure.
```kotlin
fun `create expected boolean result and assert on that`() {
  val expected: Boolean = Calculator.sum(2, 2) == 4
  expected shouldBe true
}
```

## Dependency injection, Koin

See [Koin: dependency injection](koin-dependency-injection.md#testing-with-koin) docs
for how to test code that required dependencies injected.

## Mocking

We use [Mockk](https://mockk.io/) for mocking and spying.

The most important things to know is that the `mockk` function creates a mock,
`every` specifies its behavior, and `verify` asserts that it was called.

A basic example
```kotlin
val calculator = mockk<Calculator>()
every { calculator.sum(any(), any()) } returns 42

System.out.println(calculator.sum(20, 22))

verify(exactly = 1) {
    calculator.sum(20, 22)
}
```

There's a few more things that you are likely to use:
```kotlin
val mock = mockk<Calculator>()

// Mock function that returns Unit (i.e. has no return value)
every { mock.reset() } just Runs

// Or set relaxUnitFun = true to have all functions that return Unit be mocked automatically as no-ops
val relaxed = mockk<Calculator>(relaxUnitFun = true)

// call real implementation
every { mock.complexFunction() } answers { callOriginal() }
```

#### Mocking extension functions

Extension functions are very common in Kotlin, but they are a bit more complicated to mock because they are static.

An extension function like:
```kotlin
// MyFile.kt
package com.flexport.customsproductservice

fun String.myToInt(): Int = this.toInt()
```

Can be mocked as follows:
```kotlin
// Note the complete file will be mocked
// You can use callOriginal() to un-mock some functions
mockkStatic("com.flexport.customsproductservice.MyFileKt")

every { any<String>().myToInt() } returns 0
// or for a specific value
every { "42".myToInt() } returns 42
```

#### Combining mocking with dependency injection

Injecting a mocked dependency is possible by defining a local module:

```kotlin
class FooServiceTest : KoinTest {
  // Suppose this is testing:
  // class FooServiceImpl(private val fooRepo: FooRepo) : FooService

  private val mockFooRepo: FooRepo = mockk {
      every { get(any()) } returns Foo("1")
  }

  @JvmField
  @RegisterExtension
  val koinTestExtension = KoinTestExtension.create {
    modules(
      // Use the module with the real FooService
      fooServiceModule,
      // And define a module specific to this test for the mocks
      module { single { mockFooRepo } },
    )
  }

  // Now this will be the real FooServiceImpl with a mocked FooRepo injected
  private val fooService: FooService by inject()
}

```

## GraphQL

See [Graphql](graphql.md#graphql-integration-testing) docs for how to create integration tests for GraphQL
