package com.tripian.trpcore.flow

import android.content.SharedPreferences
import android.os.Looper
import android.text.TextUtils
import com.tripian.trpcore.base.SuspendUseCase
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

/** The TConfig static fields TRPRest overwrites, which flow harnesses snapshot and restore. */
internal val TCONFIG_STATIC_FIELDS = listOf("appContext", "url", "key", "device", "lang")

/** Makes [mainLooper] the main and current looper and gives `TextUtils.isEmpty` its JVM behaviour. */
internal fun mockAndroidMainThread(mainLooper: Looper) {
    mockkStatic(Looper::class)
    every { Looper.getMainLooper() } returns mainLooper
    every { Looper.myLooper() } returns mainLooper
    every { mainLooper.thread } answers { Thread.currentThread() }
    mockkStatic(TextUtils::class)
    every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
}

/** Wires the base-class repositories every [SuspendUseCase] reads. */
internal fun <T : SuspendUseCase<*, *>> T.withFlowDependencies(miscRepository: MiscRepository): T = apply {
    this.miscRepository = miscRepository
    tripianUserRepository = mockk<TripianUserRepository>(relaxed = true)
}

/** Builds [ACTimelineVM] on the given real use cases and repository, with every other collaborator relaxed. */
internal fun buildFlowTimelineViewModel(
    fetchTimelineUseCase: FetchTimelineUseCase,
    updateSegmentTimeUseCase: UpdateSegmentTimeUseCase,
    updateStepTimeUseCase: UpdateStepTimeUseCase,
    timelineRepository: TimelineRepository,
    miscRepository: MiscRepository
): ACTimelineVM {
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
        this.miscRepository = miscRepository
        viewListener = mockk<ViewListener>(relaxed = true)
        setPrivateField("onboardingDispatched", true)
    }
}

/** Sets a private field declared directly on the receiver's class. */
internal fun Any.setPrivateField(name: String, value: Any) {
    val field = javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}

/** Captures a static field so a harness can put the pre-test value back, including an uninitialized lateinit. */
internal class StaticFieldSnapshot(owner: Class<*>, name: String) {
    private val field = owner.getDeclaredField(name).apply { isAccessible = true }
    private val value: Any? = field.get(null)

    fun restore() = field.set(null, value)
}

/** A process-local [SharedPreferences] that stands in for `PreferencesOne.pref` on the JVM. */
internal class InMemorySharedPreferences : SharedPreferences {
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
