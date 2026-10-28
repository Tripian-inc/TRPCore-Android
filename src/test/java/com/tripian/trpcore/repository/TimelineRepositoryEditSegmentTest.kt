package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.TimelineGenericResponse
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins that [TimelineRepository.editSegmentAsync] derives `doNotGenerate` from the
 * segmentIndex and segmentType alone, overriding whatever the caller set: 0 on create and on a
 * booked or reserved activity update, 1 on every other update.
 */
class TimelineRepositoryEditSegmentTest {

    private val trpRest: TRPRest = mockk()

    private fun repository(): TimelineRepository = TimelineRepository(trpRest = trpRest)

    private fun stubSuccess(): CapturingSlot<TimelineSegmentSettings> {
        val segmentSlot = slot<TimelineSegmentSettings>()
        every {
            trpRest.editTimelineSegment(any(), capture(segmentSlot), any(), any())
        } answers {
            val success = arg<(TimelineGenericResponse) -> Unit>(2)
            success(TimelineGenericResponse())
        }
        return segmentSlot
    }

    private suspend fun sentDoNotGenerate(index: Int?, type: String?, callerValue: Int): Int? {
        val segmentSlot = stubSuccess()
        val segment = TimelineSegmentSettings().apply {
            segmentIndex = index
            segmentType = type
            doNotGenerate = callerValue
        }
        repository().editSegmentAsync("hash", segment)
        return segmentSlot.captured.doNotGenerate
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when an itinerary segment is created success`() = runTest {
        assertEquals(0, sentDoNotGenerate(index = null, type = SegmentType.ITINERARY, callerValue = 1))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when a booked segment is created success`() = runTest {
        assertEquals(0, sentDoNotGenerate(index = null, type = SegmentType.BOOKED_ACTIVITY, callerValue = 1))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 1 when an itinerary segment is updated success`() = runTest {
        assertEquals(1, sentDoNotGenerate(index = 2, type = SegmentType.ITINERARY, callerValue = 0))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when a booked segment is updated success`() = runTest {
        assertEquals(0, sentDoNotGenerate(index = 2, type = SegmentType.BOOKED_ACTIVITY, callerValue = 1))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when a reserved segment is updated success`() = runTest {
        assertEquals(0, sentDoNotGenerate(index = 2, type = SegmentType.RESERVED_ACTIVITY, callerValue = 1))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 1 when a manual poi segment is updated success`() = runTest {
        assertEquals(1, sentDoNotGenerate(index = 2, type = SegmentType.MANUAL_POI, callerValue = 0))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 1 when a generated segment is updated success`() = runTest {
        assertEquals(1, sentDoNotGenerate(index = 2, type = SegmentType.GENERATED, callerValue = 0))
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when a manual poi segment is created success`() = runTest {
        assertEquals(0, sentDoNotGenerate(index = null, type = SegmentType.MANUAL_POI, callerValue = 1))
    }
}
