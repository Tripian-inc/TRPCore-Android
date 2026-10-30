package com.tripian.trpcore.flow.live

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.manager.AvailabilityCheckManager
import com.tripian.trpcore.domain.usecase.timeline.CreateSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.CreateTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteStepUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.GetTimelineStepRoutesUseCase
import com.tripian.trpcore.domain.usecase.timeline.ResolveCitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateSegmentTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.AddMissingBookedActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.DetectReservedToBookedTransitionUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.RemoveObsoleteSegmentsUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.ResolveCityIdsForActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.SyncReservedToBookedUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.UpdateDateRangeUseCase
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.ui.timeline.ACTimelineVM
import com.tripian.trpcore.ui.timeline.mapper.MapItemMapper
import com.tripian.trpcore.ui.timeline.mapper.TimelineDisplayItemBuilder
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.ViewListener
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
            assertEquals("doNotGenerate", doNotGenerate, request.get("doNotGenerate").asInt)
            assertEquals("available", available, request.get("available").asBoolean)
            assertEquals("distinctPlan", distinctPlan, request.get("distinctPlan").asBoolean)
        }

        /** The request's segment type and plan flags, for reports. */
        val flags: String
            get() = "segmentType=${request.get("segmentType")?.asString}, " +
                "doNotGenerate=${request.get("doNotGenerate")?.asInt}, " +
                "available=${request.get("available")?.asBoolean}, " +
                "distinctPlan=${request.get("distinctPlan")?.asBoolean}, " +
                "segmentIndex=${request.get("segmentIndex")}, " +
                "startDate=${request.get("startDate")?.asString}, " +
                "endDate=${request.get("endDate")?.asString}"
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
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
        every { mainLooper.thread } answers { Thread.currentThread() }
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

        previousShowLog = TLogger.showLog
        TLogger.showLog = false

        singletonSnapshots += StaticFieldSnapshot(TRPCore::class.java, "core")
        singletonSnapshots += TCONFIG_FIELDS.map { StaticFieldSnapshot(TConfig::class.java, it) }
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

        repository = recordingRepository(TimelineRepository(rest))
        fetchTimelineUseCase = FetchTimelineUseCase(repository).withBaseDependencies()
        updateSegmentTimeUseCase = UpdateSegmentTimeUseCase(repository).withBaseDependencies()
        updateStepTimeUseCase = UpdateStepTimeUseCase(repository).withBaseDependencies()
        waitForGenerationUseCase = WaitForGenerationUseCase(repository).withBaseDependencies()
        vm = buildViewModel()
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

    private fun <T : SuspendUseCase<*, *>> T.withBaseDependencies(): T = apply {
        miscRepository = localizedMiscRepository
        tripianUserRepository = mockk<TripianUserRepository>(relaxed = true)
    }

    private fun buildViewModel(): ACTimelineVM {
        val displayItemBuilder = mockk<TimelineDisplayItemBuilder>(relaxed = true) {
            every { build(any(), any(), any(), any(), any(), any()) } returns emptyList()
        }
        return ACTimelineVM(
            doLightLogin = mockk<DoLightLogin>(relaxed = true),
            fetchTimelineUseCase = fetchTimelineUseCase,
            createTimelineUseCase = mockk<CreateTimelineUseCase>(relaxed = true),
            createSegmentUseCase = mockk<CreateSegmentUseCase>(relaxed = true),
            waitForGenerationUseCase = mockk<WaitForGenerationUseCase>(relaxed = true),
            deleteSegmentUseCase = mockk<DeleteSegmentUseCase>(relaxed = true),
            deleteStepUseCase = mockk<DeleteStepUseCase>(relaxed = true),
            updateStepTimeUseCase = updateStepTimeUseCase,
            updateSegmentTimeUseCase = updateSegmentTimeUseCase,
            getTimelineStepRoutesUseCase = mockk<GetTimelineStepRoutesUseCase>(relaxed = true),
            resolveCitiesUseCase = mockk<ResolveCitiesUseCase>(relaxed = true),
            tripRepository = mockk<TripRepository>(relaxed = true),
            timelineRepository = repository,
            preferences = mockk<Preferences>(relaxed = true),
            resolveCityIdsForActivitiesUseCase = mockk<ResolveCityIdsForActivitiesUseCase>(relaxed = true),
            detectReservedToBookedTransitionUseCase = mockk<DetectReservedToBookedTransitionUseCase>(relaxed = true),
            syncReservedToBookedUseCase = mockk<SyncReservedToBookedUseCase>(relaxed = true),
            addMissingBookedActivitiesUseCase = mockk<AddMissingBookedActivitiesUseCase>(relaxed = true),
            updateDateRangeUseCase = mockk<UpdateDateRangeUseCase>(relaxed = true),
            removeObsoleteSegmentsUseCase = mockk<RemoveObsoleteSegmentsUseCase>(relaxed = true),
            availabilityCheckManager = mockk<AvailabilityCheckManager>(relaxed = true),
            mapItemMapper = mockk<MapItemMapper>(relaxed = true),
            displayItemBuilder = displayItemBuilder
        ).apply {
            strings = mockk<Strings>(relaxed = true)
            miscRepository = localizedMiscRepository
            viewListener = mockk<ViewListener>(relaxed = true)
            setPrivateField("onboardingDispatched", true)
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

    private fun Any.setPrivateField(name: String, value: Any) {
        val field = javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    /** Captures a static field so [after] can put the pre-test value back, including an uninitialized lateinit. */
    private class StaticFieldSnapshot(owner: Class<*>, name: String) {
        private val field = owner.getDeclaredField(name).apply { isAccessible = true }
        private val value: Any? = field.get(null)

        fun restore() = field.set(null, value)
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?) = apply { pending[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
            override fun putInt(key: String, value: Int) = apply { pending[key] = value }
            override fun putLong(key: String, value: Long) = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
            override fun remove(key: String) = apply { removed += key }
            override fun clear() = apply { clearAll = true }
            override fun commit(): Boolean {
                if (clearAll) values.clear()
                removed.forEach(values::remove)
                values.putAll(pending)
                return true
            }
            override fun apply() {
                commit()
            }
        }
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
        private val TCONFIG_FIELDS = listOf("appContext", "url", "key", "device", "lang")
        private val TOKEN_MANAGER_FIELDS = listOf("token", "loginTime", "consecutiveRefreshFailures", "lastRefreshFailureTime")
    }
}
