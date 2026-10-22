package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineSegmentSettings
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the segment payload [UpdateSegmentTimeUseCase] sends: the index identifies
 * the segment being edited, and the end date defaults to the start's day unless
 * [UpdateSegmentTimeUseCase.Params.newEndDate] names a different one.
 */
class UpdateSegmentTimeUseCaseTest {

    private val repository: TimelineRepository = mockk(relaxed = true)
    private val useCase = UpdateSegmentTimeUseCase(repository)

    private fun original(): TimelineSegment = TimelineSegment().apply {
        startDate = "2024-05-01 09:00"
        endDate = "2024-05-01 18:00"
    }

    @Test
    fun `test execute defaults newEndDate to the start day when not provided success`() = runTest {
        val core: TRPCore = mockk()
        every { core.getCurrentCurrency() } returns "USD"
        TRPCore.core = core

        val segmentSlot = slot<TimelineSegmentSettings>()
        coEvery { repository.editSegmentAsync(any(), capture(segmentSlot)) } returns Unit

        useCase(
            UpdateSegmentTimeUseCase.Params(
                tripHash = "hash",
                segmentIndex = 3,
                original = original(),
                newStartTime = "10:00",
                newEndTime = "11:00"
            )
        )

        assertEquals("2024-05-01 10:00", segmentSlot.captured.startDate)
        assertEquals("2024-05-01 11:00", segmentSlot.captured.endDate)
        assertEquals(3, segmentSlot.captured.segmentIndex)
    }

    @Test
    fun `test execute uses newEndDate as the end day when provided success`() = runTest {
        val core: TRPCore = mockk()
        every { core.getCurrentCurrency() } returns "USD"
        TRPCore.core = core

        val segmentSlot = slot<TimelineSegmentSettings>()
        coEvery { repository.editSegmentAsync(any(), capture(segmentSlot)) } returns Unit

        useCase(
            UpdateSegmentTimeUseCase.Params(
                tripHash = "hash",
                segmentIndex = 3,
                original = original(),
                newStartTime = "23:00",
                newEndTime = "01:00",
                newEndDate = "2024-05-02"
            )
        )

        assertEquals("2024-05-01 23:00", segmentSlot.captured.startDate)
        assertEquals("2024-05-02 01:00", segmentSlot.captured.endDate)
    }
}
