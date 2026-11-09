package com.tripian.trpcore.ui.splash

import android.os.Bundle
import android.os.Looper
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.host.HostStrategy
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.usecase.timeline.LookupTourProductUseCase
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.ViewListener
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.one.api.tour.model.TourProductLookupResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Pins that the host-start splash applies the host language and waits for its
 * translations before opening My Trips / create-trip, and that a failed or
 * timed-out load still navigates instead of leaving the splash hanging.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ACSplashVMLanguageTest {

    private val miscRepository: MiscRepository = mockk(relaxed = true)
    private val timelineRepository: TimelineRepository = mockk(relaxed = true)
    private val mainLooper: Looper = mockk(relaxed = true)
    private val originalHost = TRPCore.host

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
        every { mainLooper.thread } returns Thread.currentThread()
        TRPCore.host = mockk<HostStrategy>(relaxed = true) {
            every { createsTimelineFromScratchOnEmpty() } returns true
        }
        coEvery { timelineRepository.getUserTimelinesAsync(any(), any(), any()) } returns emptyList()
        every { miscRepository.isLanguagesLoaded } returns true
    }

    @After
    fun tearDown() {
        TRPCore.host = originalHost
        unmockkStatic(Looper::class)
        Dispatchers.resetMain()
    }

    private fun vm(
        doLightLogin: DoLightLogin = mockk(relaxed = true),
        tripRepository: TripRepository = mockk(relaxed = true),
        lookupTourProductUseCase: LookupTourProductUseCase = mockk(relaxed = true),
        reservations: String? = null
    ): ACSplashVM {
        val vm = ACSplashVM(
            doLightLogin = doLightLogin,
            tripRepository = tripRepository,
            timelineRepository = timelineRepository,
            lookupTourProductUseCase = lookupTourProductUseCase,
            preferences = mockk<Preferences>(relaxed = true)
        )
        vm.strings = mockk<Strings>(relaxed = true)
        vm.miscRepository = miscRepository
        vm.arguments = mockk<Bundle> {
            every { getString(any()) } returns null
            every { getString("language") } returns "es"
            every { getString("reservations") } returns reservations
        }
        return vm
    }

    private fun countItineraryEvents(vm: ACSplashVM): () -> Int {
        var count = 0
        vm.onItineraryReady.observeForever { count++ }
        return { count }
    }

    private fun countCreateTripEvents(vm: ACSplashVM): () -> Int {
        var count = 0
        vm.onShowCreateTrip.observeForever { count++ }
        return { count }
    }

    private fun countMyTripsEvents(vm: ACSplashVM): () -> Int {
        var count = 0
        vm.onShowMyTrips.observeForever { count++ }
        return { count }
    }

    @Test
    fun hostLanguageIsAppliedAndAwaitedBeforeNavigation() = runTest {
        val translations = CompletableDeferred<Boolean>()
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } coAnswers { translations.await() }
        val vm = vm()
        val createTripEvents = countCreateTripEvents(vm)

        vm.onViewCreated(null)
        runCurrent()

        verify { miscRepository.changeLanguage("es") }
        assertEquals(0, createTripEvents())

        translations.complete(true)
        runCurrent()

        assertEquals(1, createTripEvents())
        coVerify(exactly = 1) { miscRepository.waitForLanguagesLoadedAsync() }
    }

    @Test
    fun failedLanguageLoadIsRetriedOnceAndStillNavigates() = runTest {
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } returns false
        val vm = vm()
        val createTripEvents = countCreateTripEvents(vm)

        vm.onViewCreated(null)
        runCurrent()

        assertEquals(1, createTripEvents())
        coVerify(exactly = 2) { miscRepository.waitForLanguagesLoadedAsync() }
    }

    @Test
    fun timedOutLanguageLoadStillNavigatesAfterOneRetry() = runTest {
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } coAnswers { awaitCancellation() }
        val vm = vm()
        val createTripEvents = countCreateTripEvents(vm)

        vm.onViewCreated(null)
        advanceTimeBy(30_001)
        runCurrent()

        assertEquals(0, createTripEvents())

        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(1, createTripEvents())
        coVerify(exactly = 2) { miscRepository.waitForLanguagesLoadedAsync() }
    }

    @Test
    fun existingNotPastTimelineShowsMyTripsInsteadOfCreateTrip() = runTest {
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } returns true
        coEvery { timelineRepository.getUserTimelinesAsync(any(), any(), any()) } returns
            listOf(mockk<Timeline>(relaxed = true))
        val vm = vm()
        val createTripEvents = countCreateTripEvents(vm)
        val myTripsEvents = countMyTripsEvents(vm)

        vm.onViewCreated(null)
        runCurrent()

        assertEquals(1, myTripsEvents())
        assertEquals(0, createTripEvents())
    }

    @Test
    fun reservationsOpenTheItineraryWithoutWaitingForTranslations() = runTest {
        val translations = CompletableDeferred<Boolean>()
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } coAnswers { translations.await() }
        val lookup = mockk<LookupTourProductUseCase> {
            coEvery { this@mockk.invoke(any()) } returns mockk<TourProductLookupResponse> {
                every { data } returns TourProduct().apply { cityId = 42 }
            }
        }
        val tripRepository = mockk<TripRepository>(relaxed = true) {
            every { getCachedCityById(42) } returns mockk<City>(relaxed = true)
        }
        val viewListener = mockk<ViewListener>(relaxed = true)
        val vm = vm(
            tripRepository = tripRepository,
            lookupTourProductUseCase = lookup,
            reservations = "[{\"ProductId\":123,\"Title\":\"Tour\",\"Date\":\"2026-12-01T10:00:00\"}]"
        )
        vm.viewListener = viewListener
        val itineraryEvents = countItineraryEvents(vm)
        val createTripEvents = countCreateTripEvents(vm)

        vm.onViewCreated(null)
        runCurrent()

        assertFalse(translations.isCompleted)
        assertEquals(1, itineraryEvents())
        assertEquals(0, createTripEvents())
        verify(exactly = 0) { viewListener.finishActivity() }
    }

    @Test
    fun lightLoginFailureFinishesWithoutWaitingForTranslations() = runTest {
        val translations = CompletableDeferred<Boolean>()
        coEvery { miscRepository.waitForLanguagesLoadedAsync() } coAnswers { translations.await() }
        val doLightLogin = mockk<DoLightLogin> {
            coEvery { this@mockk.invoke(any()) } throws RuntimeException("light login failed")
        }
        val viewListener = mockk<ViewListener>(relaxed = true)
        val vm = vm(doLightLogin = doLightLogin)
        vm.viewListener = viewListener
        val itineraryEvents = countItineraryEvents(vm)
        val createTripEvents = countCreateTripEvents(vm)
        val myTripsEvents = countMyTripsEvents(vm)

        vm.onViewCreated(null)
        runCurrent()

        assertFalse(translations.isCompleted)
        verify(exactly = 1) { viewListener.finishActivity() }
        assertEquals(0, itineraryEvents())
        assertEquals(0, createTripEvents())
        assertEquals(0, myTripsEvents())
    }
}
