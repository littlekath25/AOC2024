# gRPC and Protobuf

# Local development

## Querying the service using Postman

If you want to manually call the gRPC endpoint, [Postman](https://www.postman.com/downloads/) is a pretty good choice.
To use Postman to query your service you'll need to:

- Create a new gRPC request
  - Click **New**
  - Click **gRPC Request**
- Point to your local gRPC server (usually `localhost:50051`)
- Click **Select a method** and chose **Server Reflection**
- You now have all gRPC endpoints listed and ready to invoke!

Pro tip: Try clicking **Use Example Message** at the bottom of the message input box to get example input data

# Calling another gRPC service

### Add a protobuf dependency

In order to call another gRPC service, add the protobuf files of the service you want to call
to `stub/build.gradle.kts`:

```kotlin
// If the exported library includes .proto files directly use the following:
protobuf("com.flexport.theservice", "the-service-api-kt", "1.0.0")

// If the exported library contains compiled Kotlin classes use the following
api("com.flexport.theservice", "the-service-api-kt", "1.0.0")

// If the exported library only contains the compiled Java classes use the following:
api("com.flexport.theservice", "the-service-api", "1.0.0")
api("org.jetbrains.kotlinx", "kotlinx-coroutines-guava", libs.versions.kotlinx.coroutines.get())
```

### Use the grpc stub to create a client

You can now add a stub for the client as follows:

```kotlin
import com.flexport.customs.lib.grpc.client.ChannelFactory

class TheServiceClient(channelFactory: ChannelFactory) {
  // If the library included .proto files or included compiled kotlin DSL use this
  // "the_service" must be registered in the monorepo /service_registry.yml
  private val theServiceStub: TheServiceAPICoroutineStub =
    TheServiceAPICoroutineStub(channelFactory.newChannel("the_service"))

  // If the library only contains compiled Java classes use this
  // in combination with kotlinx.coroutines.guava.await
  private val theServiceStub: TheServiceAPIGrpc.TheServiceAPIFutureStub =
    TheServiceAPIGrpc.newFutureStub(channelFactory.newChannel("the_service"))

  suspend fun useTheService() {
    return theServiceStub.getSomeInfo()
  }
}
```

### Testing

If you want to write test where you mock away the external service, you can use the following boilerplate:

```kotlin
import com.asarkar.grpc.test.GrpcCleanupExtension
import com.asarkar.grpc.test.Resources
import com.flexport.customs.lib.grpc.client.ChannelFactory

@ExtendWith(GrpcCleanupExtension::class)
class TheServiceClientTest {

  private var lastReceivedRequest: GetSomeInfoRequest? = null

  private val mockService = object : TheServiceAPIGrpcKt.TheServiceAPICoroutineImplBase() {
    override suspend fun getSomeInfo(request: GetSomeInfoRequest): GetSomeInfoResponse {
      lastReceivedRequest = request
      return getSomeInfoResponse {
        // ...
      }
    }
  }
  lateinit var client: TheServiceClient

  @BeforeEach
  fun setup(resources: Resources) {
    lastReceivedRequest = null
    val channel = InProcessChannelBuilder.forName("the_service").build()
    val server = InProcessServerBuilder.forName("the_service").directExecutor()
      .addService(mockService)
      .build()
      .start()
    resources.register(channel)
    resources.register(server)

    client = TheServiceClient(ChannelFactory.returningChannel(channel))
  }

  @Test
  // You can also add resources as a parameter to each test if you want to use different mock servers
  // for each test instead of having a single shared on using @BeforeEach
  fun `the service client should get some info`(): Unit = runBlocking {
      val result = client.useTheService()

      // You can write assertions against the given mocked response
      result shouldBe /* ... */

      // You can write assertions against the request that your client send to the mock server
      lastReceivedRequest shouldBe getSomeInfoRequest {
          // ...
      }
  }
}
```

# Defining .proto files

In order to have a single authoritative source and aid an eventual migration back to the monorepo,
all Event and gRPC API `.proto` files must be placed in the monorepo `/protobuf` folder.

### Monorepo build

See [this build file for an event](https://github.com/flexport/flexport/blob/74bb86940961f7c2f72ae580554ca3bf16410282/protobuf/flexport/customsworkflowinstrumentation/customsworkflowevent/v1beta1/BUILD#L1).

```bazel
load("@rules_proto//proto:defs.bzl", "proto_library")
load("@com_flexport_bazel_rules//ruby/ruby_proto_rules:defs.bzl", "ruby_proto_library")
load("@rules_jvm_external//:defs.bzl", "java_export")
load("@com_github_grpc_grpc_kotlin//:kt_jvm_grpc.bzl", "kt_jvm_grpc_library", "kt_jvm_proto_library")
load("//build-utils:protobuf_transitive_sources/defs.bzl", "protobuf_transitive_sources")

message_protos = glob(["my_event.proto"])

# If your protobuf does not have dependencies, you can skip this definition and use my_event_proto directly
protobuf_transitive_sources(
    name = "my_transitive_proto_sources",
    srcs = [":my_event_proto"],
)

proto_library(
    name = "my_event_proto",
    srcs = message_protos,
    strip_import_prefix = "/protobuf",
    visibility = ["//visibility:public"],
    deps = [
        #...
    ],
)

kt_jvm_proto_library(
    name = "my_event_kotlin_proto",
    visibility = ["//visibility:public"],
    deps = [":my_event_proto"],
)

# To create gRPC kotlin bindings if your *.proto file has service definition
kt_jvm_grpc_library(
    name = "my_event_kotlin_grpc",
    srcs = [":my_event_proto"],
    visibility = ["//visibility:public"],
    deps = [":my_event_kotlin_proto"],
)


# TODO: kt_jvm_export is a better fit, but not yet available in the current monorepo @rules_jvm_external version
java_export(
    name = "exported_my_event_kotlin_proto",
    maven_coordinates = "com.flexport.my.package:my-event-kt:1.0.0-beta.1.0",
    # Add the original .proto files to the artifact as resources
    # If your protobuf does not have dependencies, you should use this line:
    resource_strip_prefix = "protobuf/",
    # If your protobuf does have dependencies, you should use this line:
    resource_strip_prefix = "protobuf/flexport/myModule/myEvent/v1beta1/",
    resources = [":my_transitive_proto_sources"],
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":my_event_kotlin_proto",
        ":my_event_kotlin_grpc",
        "@maven//:javax_annotation_javax_annotation_api",
    ],
)

```

You can build locally with

```
bazel run --define "maven_repo=file://$HOME/.m2/repository" //protobuf/flexport/%directory%/%subdirectory%/v1beta1:exported_my_event_kotlin_proto.publish
```

This will create the build artifacts and put them into your local maven repository `$HOME/.m2/repository`

### Service build

Add the following line in `stub/build.gradle.kts` under the `dependencies` section:

`protobuf("com.flexport.my.package", "my-event-kt", "1.0.0-beta.1.0")`

When developing locally, gradle will first look into the local maven repository,
and only then will it look elsewhere. Thus, in case the above gradle dependency pulls old files, make sure to clean the
local maven repository.

### Note on import with transitive dependencies

When trying to import a file that has other external dependencies with `protobuf("...", "...", "...")`, messages like
the following will start appearing in your console:

![Slack thread with protobuf import errors](../docs/images/slack-thread-protobuf-transitive-import.png)
[Slack thread](https://flexport.slack.com/archives/C051Z0379G9/p1681161241271489?thread_ts=1680822574.676299&cid=C051Z0379G9)

As mentioned in the slack thread, one needs to import all the external dependencies manually.

However, all external dependencies can be pulled with one protobuf gradle import by making use of
`protobuf_transitive_sources` function in the build file as indicated in the [Monorepo Build example](#monorepo-build)

[Core side example PR ](https://github.com/flexport/flexport/pull/181180)

[NIS side example](https://github.com/flexport/customs-log-aggregator/commit/bb1c45e9a4eb6f7bd5fb57d91e61d7a3dd4a8f58#diff-a5da1f9e4b6548c1f1add99d7acfb734c385f9e9e2ed7d82f756de9670c9a1f0)

#### Note on importing `google/types` protos

While importing and using any proto
from `google/types` [in this location](https://github.com/flexport/flexport/blob/6fb9f54d7c219d5a0d925bbee88a67c4870f9900/protobuf/google/type),
`exclude_prefixes = ["google/"]` should be omitted and `resource_strip_prefix` should be set to the package name.
Example [PR](https://github.com/flexport/flexport/pull/200364/files)

### Prototyping inside the service

If you add a `.proto` file to `/stub/src/main/proto/` folder it will get compiled for use in your project.
The path from there will be the same as in monorepo `/protobuf`, for
example `/stub/src/main/proto/flexport/customs/myservice/myfile.proto`.

However, this should only be used for prototyping.
If an application is deployed, the proto files come from the monorepo.
