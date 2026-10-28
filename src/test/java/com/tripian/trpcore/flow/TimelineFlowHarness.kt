package com.tripian.trpcore.flow

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tripian.one.TRPRest
import com.tripian.one.TokenManager
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.users.model.Device
import com.tripian.one.network.TConfig
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

/**
 * JUnit rule that runs [ACTimelineVM] on the real Fetch/UpdateSegmentTime/UpdateStepTime use cases,
 * the real [TimelineRepository] and the real TRPRest/Retrofit/OkHttp stack against a [MockWebServer],
 * so a scenario can assert the request on the wire and the state the user sees.
 * `TRPCore.core` is replaced by a relaxed mock reporting "USD", because the SDK singleton cannot be
 * built on the JVM and the flow only reads the currency and app config from it.
 * Every process-wide singleton the flow touches (`TRPCore.core`, the TConfig fields TRPRest sets,
 * `PreferencesOne.pref`, TokenManager, `TLogger.showLog`, the main dispatcher and the static mocks)
 * is configured in [before] and restored or reset in [after].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineFlowHarness : ExternalResource() {

    /** A request the client sent, with its JSON body parsed when present. */
    class RecordedCall(val method: String, val path: String, val query: Map<String, String?>, val body: JsonObject?) {
        val pathWithoutQuery: String get() = path.substringBefore('?')

        /** Asserts the segment body's `doNotGenerate`, `available` and `distinctPlan` fields. */
        fun assertPlanFlags(doNotGenerate: Int, available: Boolean, distinctPlan: Boolean) {
            val segment = body ?: throw AssertionError("$method $pathWithoutQuery has no JSON body")
            assertEquals("doNotGenerate", doNotGenerate, segment.get("doNotGenerate").asInt)
            assertEquals("available", available, segment.get("available").asBoolean)
            assertEquals("distinctPlan", distinctPlan, segment.get("distinctPlan").asBoolean)
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var rest: TRPRest
    private lateinit var timelineRepository: TimelineRepository
    private lateinit var fetchTimelineUseCase: FetchTimelineUseCase
    private lateinit var updateSegmentTimeUseCase: UpdateSegmentTimeUseCase
    private lateinit var updateStepTimeUseCase: UpdateStepTimeUseCase
    lateinit var vm: ACTimelineVM
        private set

    private val mainLooper: Looper = mockk(relaxed = true)
    private var previousShowLog = true
    private val singletonSnapshots = mutableListOf<StaticFieldSnapshot>()
    private var loadedTripHash: String? = null

    private val localizedMiscRepository: MiscRepository = mockk(relaxed = true) {
        every { getLanguageValueForKey(any()) } answers { localized(firstArg()) }
    }

    /** The text the VM shows for [key]; the localization mock returns `localized:<key>`. */
    fun localized(key: String): String = "localized:$key"

    override fun before() {
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
            every { getCurrentCurrency() } returns "USD"
        }

        rest = TRPRest(
            appContext = mockk<Context>(relaxed = true),
            url = startServer(),
            key = MOCK_API_KEY,
            device = Device(
                deviceId = "flow-test-device",
                bundleId = "com.tripian.trpcore.flowtest",
                osVersion = "0",
                deviceOs = "android"
            )
        )
        singletonSnapshots += StaticFieldSnapshot(PreferencesOne::class.java, "pref")
        TConfig.lang = "en"
        PreferencesOne.pref = InMemorySharedPreferences()
        TokenManager.clear()

        timelineRepository = TimelineRepository(rest)
        fetchTimelineUseCase = FetchTimelineUseCase(timelineRepository).withBaseDependencies()
        updateSegmentTimeUseCase = UpdateSegmentTimeUseCase(timelineRepository).withBaseDependencies()
        updateStepTimeUseCase = UpdateStepTimeUseCase(timelineRepository).withBaseDependencies()
        vm = buildViewModel()
    }

    override fun after() {
        try {
            if (::vm.isInitialized) vm.viewModelScope.cancel()
            if (::rest.isInitialized) rest.job.cancel()
            if (::server.isInitialized) {
                drainUnexpectedAuthRequests()
                server.shutdown()
            }
        } finally {
            TokenManager.clear()
            singletonSnapshots.asReversed().forEach { it.restore() }
            singletonSnapshots.clear()
            TLogger.showLog = previousShowLog
            Dispatchers.resetMain()
            unmockkAll()
        }
    }

    private fun startServer(): String {
        server = MockWebServer()
        server.dispatcher = QueueDispatcher().apply {
            setFailFast(MockResponse().setResponseCode(UNEXPECTED_REQUEST_CODE).setBody("{}"))
        }
        server.start()
        return server.url(MOCK_BASE_PATH).toString()
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
            timelineRepository = timelineRepository,
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

    /** Queues one mock response; responses are served in FIFO order. */
    fun enqueueJson(code: Int, body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    /** Takes the next request the client sent and fails if it is an auth call or never arrives. */
    fun takeRequest(): RecordedCall {
        val recorded = server.takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ?: throw AssertionError("Expected another request to the mock backend, but none arrived")
        val path = recorded.path.orEmpty()
        assertNotAuthPath(path)
        val url = recorded.requestUrl
        val query = url?.queryParameterNames?.associateWith { url.queryParameter(it) }.orEmpty()
        val bodyText = recorded.body.readUtf8()
        val body = bodyText.takeIf { it.isNotBlank() }?.let { JsonParser.parseString(it).asJsonObject }
        return RecordedCall(recorded.method.orEmpty(), path, query, body)
    }

    /** Fails when the client sent anything beyond the requests the scenario already took. */
    fun assertNoMoreRequests() {
        val extra = server.takeRequest(0, TimeUnit.MILLISECONDS)
        if (extra != null) {
            fail("Unexpected extra request: ${extra.method} ${extra.path?.substringBefore('?')}")
        }
    }

    /** Serves [timeline] for the initial GET, loads it into the VM and asserts the GET on the wire. */
    fun loadTimeline(timeline: Timeline) {
        loadedTripHash = timeline.tripHash
        vm.setPrivateField("_tripHash", timeline.tripHash)
        enqueueJson(200, TimelineFixtures.timelineResponseJson(timeline))
        vm.fetchTimeline()
        awaitIdle()
        val get = takeRequest()
        assertEquals("GET", get.method)
        assertEquals("${MOCK_BASE_PATH}timeline/${timeline.tripHash}", get.pathWithoutQuery)
        assertEquals("en", get.query["lang"])
        assertTrue("Timeline did not load: ${vm.error.value}", vm.timeline.value != null)
    }

    /** Asserts the next request is the timeline re-fetch for the loaded trip and that nothing follows it. */
    fun assertTimelineReloaded() {
        val get = takeRequest()
        assertEquals("GET", get.method)
        assertEquals("${MOCK_BASE_PATH}timeline/$loadedTripHash", get.pathWithoutQuery)
        assertNoMoreRequests()
    }

    /** Asserts the change-time sheet was closed as a success with no error shown. */
    fun assertChangeFinished() {
        assertEquals(true, vm.changeTimeFinished.value)
        assertNull(vm.error.value)
    }

    /** Blocks until every coroutine the VM launched has completed. */
    fun awaitIdle() {
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

    private fun drainUnexpectedAuthRequests() {
        while (true) {
            val leftover = server.takeRequest(0, TimeUnit.MILLISECONDS) ?: return
            assertNotAuthPath(leftover.path.orEmpty())
        }
    }

    private fun assertNotAuthPath(path: String) {
        if (path.removePrefix(MOCK_BASE_PATH).startsWith("auth/")) {
            fail("Unexpected auth request ${path.substringBefore('?')}: the flow must not log in or refresh the token")
        }
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
        const val MOCK_BASE_PATH = "/prod/"
        private const val MOCK_API_KEY = "flow-test-key"
        private const val UNEXPECTED_REQUEST_CODE = 599
        private const val REQUEST_TIMEOUT_SECONDS = 5L
        private const val IDLE_TIMEOUT_MILLIS = 15_000L
        private val TCONFIG_FIELDS = listOf("appContext", "url", "key", "device", "lang")
    }
}
