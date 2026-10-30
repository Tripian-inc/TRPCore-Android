package com.tripian.trpcore.flow.live

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tripian.one.TRPRest
import com.tripian.one.TokenManager
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.one.api.timeline.model.TimelineSettings
import com.tripian.one.api.users.model.Device
import com.tripian.one.api.users.model.LightLoginRequest
import com.tripian.one.network.TConfig
import com.tripian.one.util.BaseResponse
import com.tripian.one.util.PreferencesOne
import com.tripian.one.util.TLogger
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateSegmentTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.flow.InMemorySharedPreferences
import com.tripian.trpcore.flow.StaticFieldSnapshot
import com.tripian.trpcore.flow.TCONFIG_STATIC_FIELDS
import com.tripian.trpcore.flow.buildFlowTimelineViewModel
import com.tripian.trpcore.flow.mockAndroidMainThread
import com.tripian.trpcore.flow.setPrivateField
import com.tripian.trpcore.flow.withFlowDependencies
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.ui.timeline.ACTimelineVM
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume
import org.junit.rules.ExternalResource
import retrofit2.HttpException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * JUnit rule that runs [ACTimelineVM] on the real Fetch/UpdateSegmentTime/UpdateStepTime use cases,
 * the real [TimelineRepository] and TRPRest against the live Tripian API described by [LiveBackendConfig].
 * Each test light-logs in as a fresh guest; every timeline created through [createTimeline] is deleted in [after].
 * The singletons the flow touches are snapshotted in [before] and restored in [after]; HTTP logging stays off.
 * The test is skipped when no live configuration is available.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveTimelineHarness : ExternalResource() {

    /** A segment PUT as sent, with `doNotGenerate` already derived by the repository, and the backend's answer. */
    class SegmentCall(val request: JsonObject, failure: Throwable?) {
        val outcome = BackendOutcome.of(failure)

        /** Asserts the request's `doNotGenerate`, `available` and `distinctPlan` fields. */
        fun assertPlanFlags(doNotGenerate: Int, available: Boolean, distinctPlan: Boolean) {
            assertEquals("doNotGenerate in $flags", doNotGenerate, field("doNotGenerate")?.asInt)
            assertEquals("available in $flags", available, field("available")?.asBoolean)
            assertEquals("distinctPlan in $flags", distinctPlan, field("distinctPlan")?.asBoolean)
        }

        /** The request's segment type, plan flags, index and dates, for reports. */
        val flags: String
            get() = "segmentType=${field("segmentType")?.asString}, " +
                "doNotGenerate=${field("doNotGenerate")?.asInt}, " +
                "available=${field("available")?.asBoolean}, " +
                "distinctPlan=${field("distinctPlan")?.asBoolean}, " +
                "segmentIndex=${field("segmentIndex")?.asInt}, " +
                "startDate=${field("startDate")?.asString}, " +
                "endDate=${field("endDate")?.asString}"

        private fun field(name: String): JsonElement? = request.get(name)?.takeUnless { it.isJsonNull }
    }

    /** A step PUT as sent and the backend's answer. */
    class StepCall(val stepId: Int, val startTime: String?, val endTime: String?, failure: Throwable?) {
        val outcome = BackendOutcome.of(failure)
    }

    /** How the backend answered one call: accepted, or rejected with an HTTP status and the error body's `message`. */
    class BackendOutcome private constructor(val accepted: Boolean, val httpStatus: Int?, val backendMessage: String?) {

        override fun toString(): String = when {
            accepted -> "accepted (2xx)"
            else -> "rejected: HTTP ${httpStatus ?: "n/a"}, backend message: ${backendMessage ?: "<none>"}"
        }

        companion object {
            private val gson = Gson()

            /** Reads the error body without consuming it, so the VM's error mapping still sees it. */
            fun of(failure: Throwable?): BackendOutcome {
                if (failure == null) return BackendOutcome(true, null, null)
                val http = failure as? HttpException
                    ?: return BackendOutcome(false, null, "${failure.javaClass.simpleName}: ${failure.message}")
                val body = runCatching { http.response()?.errorBody()?.source()?.peek()?.readUtf8() }.getOrNull()
                val message = runCatching { gson.fromJson(body, BaseResponse::class.java)?.message }.getOrNull()
                return BackendOutcome(false, http.code(), message?.takeIf { it.isNotBlank() } ?: body)
            }
        }
    }

    lateinit var config: LiveBackendConfig
        private set
    lateinit var vm: ACTimelineVM
        private set

    private lateinit var rest: TRPRest
    private lateinit var backend: TimelineRepository
    private lateinit var repository: TimelineRepository
    private lateinit var fetchTimelineUseCase: FetchTimelineUseCase
    private lateinit var updateSegmentTimeUseCase: UpdateSegmentTimeUseCase
    private lateinit var updateStepTimeUseCase: UpdateStepTimeUseCase
    private lateinit var waitForGenerationUseCase: WaitForGenerationUseCase

    private val mainLooper: Looper = mockk(relaxed = true)
    private var previousShowLog: Boolean? = null
    private val singletonSnapshots = mutableListOf<StaticFieldSnapshot>()
    private val createdTripHashes = mutableListOf<String>()
    private val recordedSegmentCalls = CopyOnWriteArrayList<SegmentCall>()
    private val recordedStepCalls = CopyOnWriteArrayList<StepCall>()
    private val gson = Gson()

    private val localizedMiscRepository: MiscRepository = mockk(relaxed = true) {
        every { getLanguageValueForKey(any()) } answers { "localized:${firstArg<String>()}" }
    }

    /** Every segment PUT sent so far, in order. */
    val segmentCalls: List<SegmentCall> get() = recordedSegmentCalls.toList()

    /** Every step PUT sent so far, in order. */
    val stepCalls: List<StepCall> get() = recordedStepCalls.toList()

    override fun before() {
        val loaded = LiveBackendConfig.loadOrNull()
        Assume.assumeTrue("No ${LiveBackendConfig.FILE_NAME} or TRIPIAN_LIVE_* environment: live flow tests skipped", loaded != null)
        config = loaded!!
        try {
            setUp()
        } catch (t: Throwable) {
            after()
            throw t
        }
    }

    private fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockAndroidMainThread(mainLooper)

        previousShowLog = TLogger.showLog
        TLogger.showLog = false

        singletonSnapshots += StaticFieldSnapshot(TRPCore::class.java, "core")
        singletonSnapshots += TCONFIG_STATIC_FIELDS.map { StaticFieldSnapshot(TConfig::class.java, it) }
        TRPCore.core = mockk(relaxed = true) {
            every { getCurrentCurrency() } returns CURRENCY
        }

        val runId = UUID.randomUUID().toString()
        rest = TRPRest(
            appContext = mockk<Context>(relaxed = true),
            url = config.baseUrl,
            key = config.apiKey,
            device = Device(
                deviceId = runId,
                bundleId = BUNDLE_ID,
                osVersion = OS_VERSION,
                deviceOs = "android"
            )
        )
        singletonSnapshots += StaticFieldSnapshot(PreferencesOne::class.java, "pref")
        TConfig.lang = "en"
        PreferencesOne.pref = InMemorySharedPreferences()
        singletonSnapshots += TOKEN_MANAGER_FIELDS.map { StaticFieldSnapshot(TokenManager::class.java, it) }
        TokenManager.clear()

        lightLogin("livetest-$runId")

        backend = TimelineRepository(rest)
        repository = recordingRepository(backend)
        fetchTimelineUseCase = FetchTimelineUseCase(repository).withFlowDependencies(localizedMiscRepository)
        updateSegmentTimeUseCase = UpdateSegmentTimeUseCase(repository).withFlowDependencies(localizedMiscRepository)
        updateStepTimeUseCase = UpdateStepTimeUseCase(repository).withFlowDependencies(localizedMiscRepository)
        waitForGenerationUseCase = WaitForGenerationUseCase(repository).withFlowDependencies(localizedMiscRepository)
        vm = buildFlowTimelineViewModel(
            fetchTimelineUseCase,
            updateSegmentTimeUseCase,
            updateStepTimeUseCase,
            repository,
            localizedMiscRepository
        )
    }

    override fun after() {
        try {
            if (::vm.isInitialized) vm.viewModelScope.cancel()
            deleteCreatedTimelines()
            if (::rest.isInitialized) rest.job.cancel()
        } finally {
            singletonSnapshots.asReversed().forEach { it.restore() }
            singletonSnapshots.clear()
            previousShowLog?.let { TLogger.showLog = it }
            previousShowLog = null
            Dispatchers.resetMain()
            unmockkAll()
        }
    }

    private fun lightLogin(uniqueId: String) {
        val result = CompletableDeferred<Throwable?>()
        rest.lightLogin(
            LightLoginRequest().apply { this.uniqueId = uniqueId },
            success = { result.complete(null) },
            error = { result.complete(it ?: IllegalStateException("Light login failed without a cause")) }
        )
        val failure = runBlocking { withTimeout(REQUEST_TIMEOUT_MILLIS) { result.await() } }
        if (failure != null) throw AssertionError("Light login ${BackendOutcome.of(failure)}")
        assertFalse("Light login returned no id token", TokenManager.token?.idToken.isNullOrEmpty())
    }

    private fun recordingRepository(real: TimelineRepository): TimelineRepository = spyk(real).also { spy ->
        coEvery { spy.editSegmentAsync(any(), any()) } coAnswers {
            val segment = secondArg<TimelineSegmentSettings>()
            val result = runCatching { real.editSegmentAsync(firstArg(), segment) }
            recordedSegmentCalls += SegmentCall(gson.toJsonTree(segment).asJsonObject, result.exceptionOrNull())
            result.getOrThrow()
        }
        coEvery { spy.editStepAsync(any(), any(), any(), any(), any()) } coAnswers {
            val result = runCatching { real.editStepAsync(firstArg(), secondArg(), thirdArg(), arg(3), arg(4)) }
            recordedStepCalls += StepCall(firstArg(), thirdArg(), arg(3), result.exceptionOrNull())
            result.getOrThrow()
        }
    }

    /** Creates a timeline on the backend and remembers its hash so [after] deletes it. */
    fun createTimeline(settings: TimelineSettings): Timeline {
        val timeline = runCatching { blockingCall { repository.createTimelineAsync(settings) } }
            .getOrElse { throw AssertionError("Creating the timeline was ${BackendOutcome.of(it)}") }
        timeline.tripHash.takeIf { it.isNotBlank() }?.let { createdTripHashes += it }
            ?: throw AssertionError("The backend created a timeline without a trip hash")
        return timeline
    }

    /** Adds [segment] to the trip as a new segment and returns the recorded call; a rejection is returned, not thrown. */
    fun addSegment(tripHash: String, segment: TimelineSegmentSettings): SegmentCall {
        segment.segmentIndex = null
        val sentBefore = recordedSegmentCalls.size
        val result = runCatching { blockingCall { repository.editSegmentAsync(tripHash, segment) } }
        return recordedSegmentCalls.getOrNull(sentBefore)
            ?: SegmentCall(gson.toJsonTree(segment).asJsonObject, result.exceptionOrNull() ?: IllegalStateException("Segment call was not recorded"))
    }

    /** Polls the trip until every itinerary plan has been generated, as the production create flow does. */
    fun awaitGeneration(tripHash: String): Timeline =
        blockingCall(GENERATION_TIMEOUT_MILLIS) { waitForGenerationUseCase(WaitForGenerationUseCase.Params(tripHash)) }

    /** Loads [tripHash] into the VM through its initial fetch and returns the timeline the VM published. */
    fun loadTimeline(tripHash: String): Timeline {
        vm.setPrivateField("_tripHash", tripHash)
        vm.fetchTimeline()
        awaitIdle()
        return vm.timeline.value ?: throw AssertionError("Timeline $tripHash did not load: ${lastErrorMessage()}")
    }

    /** Fetches [tripHash] straight from the backend, bypassing the VM and any local fallback it applies. */
    fun fetchTimeline(tripHash: String): Timeline =
        runCatching { blockingCall { backend.fetchTimelineAsync(tripHash) } }
            .getOrElse { throw AssertionError("Fetching timeline $tripHash was ${BackendOutcome.of(it)}") }

    /**
     * Polls until the VM closes the inline change-time sheet, then waits for its reload to finish.
     * Returns true when the change was reported as a success.
     */
    fun awaitChangeFinished(timeoutSeconds: Long = CHANGE_TIMEOUT_SECONDS): Boolean {
        val deadline = System.nanoTime() + timeoutSeconds * 1_000_000_000L
        while (vm.changeTimeFinished.value == null) {
            if (System.nanoTime() > deadline) {
                throw AssertionError("The change-time sheet did not finish within ${timeoutSeconds}s")
            }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        awaitIdle()
        return vm.changeTimeFinished.value == true
    }

    /** The error text the VM currently shows, which carries the backend's `message` on a rejected call. */
    fun lastErrorMessage(): String? = vm.error.value

    /** Writes one result line to the test's standard output, where the live run report is collected from. */
    fun report(line: String) = println("[live] $line")

    private fun awaitIdle() {
        val scopeJob = vm.viewModelScope.coroutineContext[Job] ?: return
        runBlocking {
            withTimeout(IDLE_TIMEOUT_MILLIS) {
                while (true) {
                    val active = scopeJob.children.filter { it.isActive }.toList()
                    if (active.isEmpty()) break
                    active.joinAll()
                }
            }
        }
    }

    private fun <T> blockingCall(timeoutMillis: Long = REQUEST_TIMEOUT_MILLIS, block: suspend () -> T): T =
        runBlocking { withTimeout(timeoutMillis) { block() } }

    private fun deleteCreatedTimelines() {
        if (!::repository.isInitialized) return
        createdTripHashes.forEach { hash ->
            try {
                blockingCall { repository.deleteTimelineAsync(hash) }
                report("deleted trip $hash")
            } catch (t: Throwable) {
                report("UNDELETED trip $hash: ${BackendOutcome.of(t)}")
            }
        }
        createdTripHashes.clear()
    }


    companion object {
        const val CURRENCY = "USD"
        private const val BUNDLE_ID = "com.tripian.app"
        private const val OS_VERSION = "35"
        private const val REQUEST_TIMEOUT_MILLIS = 30_000L
        private const val GENERATION_TIMEOUT_MILLIS = 90_000L
        private const val IDLE_TIMEOUT_MILLIS = 60_000L
        private const val CHANGE_TIMEOUT_SECONDS = 30L
        private const val POLL_INTERVAL_MILLIS = 100L
        private val TOKEN_MANAGER_FIELDS = listOf("token", "loginTime", "consecutiveRefreshFailures", "lastRefreshFailureTime")
    }
}
