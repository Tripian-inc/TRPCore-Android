# Flow tests

Flow tests live in `src/test/java/com/tripian/trpcore/flow/`. They drive a real screen ViewModel
through the real use cases, repository and TRPRest/Retrofit/OkHttp stack against an OkHttp
`MockWebServer`. A scenario asserts both the request on the wire (path, method, `lang`, JSON body)
and what the user sees (`error`, `changeTimeFinished`, `timeline`).

## Harness

`TimelineFlowHarness` is a JUnit rule (`@get:Rule val harness = TimelineFlowHarness()`).
It starts the server, configures TConfig/TokenManager/PreferencesOne, replaces `TRPCore.core`
with a relaxed mock that reports `USD`, builds `ACTimelineVM` with the real Fetch, UpdateSegmentTime
and UpdateStepTime use cases. Afterwards it restores `TRPCore.core`, the TConfig fields, `PreferencesOne.pref`
and `TLogger.showLog`, clears TokenManager, resets the main dispatcher and undoes the static mocks.
Localized strings come back as `localized:<key>`; compare against `harness.localized(LanguageConst.<KEY>)`.

- `loadTimeline(timeline)`: serves the initial GET and loads it into the VM.
- `enqueueJson(code, body)`: queues the next response (FIFO).
- `takeRequest()`: next recorded request with parsed body. Any request without a queued response,
  including an auth call, gets a 599 from the harness dispatcher, so the flow fails fast.
- `assertNoMoreRequests()`: fails if the client sent anything else.
- `assertTimelineReloaded()`: the next request is the timeline GET and nothing follows it.
- `assertChangeFinished()`: the change-time sheet closed as a success with no error.
- `awaitIdle()`: waits for every coroutine the VM launched.
- `TimelineFixtures`: builds TRPOne models and their Gson payloads.

## Running

From the host app that includes `:TRPCore` (e.g. TripianApp):
`./gradlew :TRPCore:testDebugUnitTest --tests "com.tripian.trpcore.flow.*"`

## Adding a scenario

1. Pick the class for the area (or add `<Area>FlowTest` in the `flow` package with the rule).
2. Build the timeline with `TimelineFixtures`, call `harness.loadTimeline(...)`.
3. Queue the backend answers, call the VM method as the screen does, then `harness.awaitIdle()`.
4. Assert each request with `takeRequest()`, then `assertNoMoreRequests()` and the VM state.
   Every regression fix adds its scenario in the same PR.
