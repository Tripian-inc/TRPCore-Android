package com.tripian.trpcore.ui.timeline.compose.core

import android.view.ViewGroup
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineToastAnchorTest {

    @Test
    fun testCurrentIsNullWhenNoRootsRegistered() {
        val anchor = TimelineToastAnchor()

        assertNull(anchor.current)
    }

    @Test
    fun testCurrentIsMostRecentlyRegisteredRoot() {
        val anchor = TimelineToastAnchor()
        val first = mockk<ViewGroup>()
        val second = mockk<ViewGroup>()

        anchor.register(first)
        anchor.register(second)

        assertEquals(second, anchor.current)
    }

    @Test
    fun testReregisteringMovesRootToMostRecent() {
        val anchor = TimelineToastAnchor()
        val first = mockk<ViewGroup>()
        val second = mockk<ViewGroup>()

        anchor.register(first)
        anchor.register(second)
        anchor.register(first)

        assertEquals(first, anchor.current)
    }

    @Test
    fun testUnregisteringCurrentFallsBackToPreviousRoot() {
        val anchor = TimelineToastAnchor()
        val first = mockk<ViewGroup>()
        val second = mockk<ViewGroup>()

        anchor.register(first)
        anchor.register(second)
        anchor.unregister(second)

        assertEquals(first, anchor.current)
    }

    @Test
    fun testUnregisteringAnUnknownRootIsANoOp() {
        val anchor = TimelineToastAnchor()
        val first = mockk<ViewGroup>()
        val unknown = mockk<ViewGroup>()

        anchor.register(first)
        anchor.unregister(unknown)

        assertEquals(first, anchor.current)
    }
}
