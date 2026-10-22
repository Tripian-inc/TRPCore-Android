package com.tripian.trpcore.repository

import com.tripian.one.TRPRest
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
 * Pins that [TimelineRepository.editSegmentAsync] derives `doNotGenerate` from
 * [TimelineSegmentSettings.segmentIndex] alone, overriding whatever the caller set.
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

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 1 when segmentIndex is present success`() = runTest {
        val segmentSlot = stubSuccess()
        val segment = TimelineSegmentSettings().apply {
            segmentIndex = 2
            doNotGenerate = 0
        }

        repository().editSegmentAsync("hash", segment)

        assertEquals(1, segmentSlot.captured.doNotGenerate)
    }

    @Test
    fun `test editSegmentAsync sets doNotGenerate to 0 when segmentIndex is null success`() = runTest {
        val segmentSlot = stubSuccess()
        val segment = TimelineSegmentSettings().apply {
            segmentIndex = null
            doNotGenerate = 1
        }

        repository().editSegmentAsync("hash", segment)

        assertEquals(0, segmentSlot.captured.doNotGenerate)
    }
}
