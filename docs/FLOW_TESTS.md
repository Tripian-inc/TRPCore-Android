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

## Live-backend flow tests

`flow/live/` drives the real `ACTimelineVM` against the real Tripian API to answer questions the
mock suite cannot, such as whether the backend accepts a segment or step PUT the SDK sends.
Each test light-logs in as a fresh guest (`livetest-<uuid>`), creates its own throwaway trip
(city, timeline-date segment, a generated day plan on day 1, a booked activity on day 2), runs one
change through the VM, reloads the timeline and deletes the trip in teardown. A trip that could not
be deleted is printed as `[live] UNDELETED trip <hash>`.

`LiveTimelineHarness` mirrors `TimelineFlowHarness`, but talks to a real `TRPRest`, records every
segment and step PUT with its outcome (HTTP status and the backend message verbatim) and keeps the
HTTP logging interceptor off.

### Settings

Copy `live-test.properties.example` to `live-test.properties` in the module root (gitignored):

| Key | Meaning |
|-----|---------|
| `stage` | `predev` or `prod` |
| `apiKey` | API key inline, or use the two keys below |
| `apiKeyFile` | Properties file holding the key, relative to `live-test.properties` |
| `apiKeyProperty` | Property name inside `apiKeyFile` |
| `cityId` | Optional, defaults to 109 |

```properties
stage=prod
apiKeyFile=../../TripianApp/secrets.properties
apiKeyProperty=TRIPIAN_API_KEY_PROD
```

The environment variables `TRIPIAN_LIVE_STAGE` and `TRIPIAN_LIVE_API_KEY` override the file.

### Running

The suite is excluded from every test task unless `-PliveFlow` is set:
`./gradlew :TRPCore:testDebugUnitTest -PliveFlow=true --tests 'com.tripian.trpcore.flow.live.*'`

Without a stage or a key the tests are skipped (JUnit assumption), not failed. Results and the
recorded requests are printed as `[live]` lines in the test report's standard output.

The suite registers a real guest user per test and creates and deletes real trips on the chosen
stage. Run it by hand only; it must never run in CI.
