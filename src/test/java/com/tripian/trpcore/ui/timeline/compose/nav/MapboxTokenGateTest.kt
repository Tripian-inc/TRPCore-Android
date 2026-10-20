package com.tripian.trpcore.ui.timeline.compose.nav

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapboxTokenGateTest {

    @Test
    fun testGateStartsReadyAndSkipsResolverWhenTokenAlreadyApplied() = runTest {
        val requestedIds = mutableListOf<String?>()
        val gate = MapboxTokenGate(tokenAlreadyApplied = true) { requestedIds += it; true }

        assertEquals(MapboxTokenGate.State.Ready, gate.state.value)
        gate.resolve("user-1")

        assertEquals(MapboxTokenGate.State.Ready, gate.state.value)
        assertTrue(requestedIds.isEmpty())
    }

    @Test
    fun testGateMovesFromResolvingToReadyWhenResolverSucceeds() = runTest {
        val result = CompletableDeferred<Boolean>()
        val gate = MapboxTokenGate(tokenAlreadyApplied = false) { result.await() }

        assertEquals(MapboxTokenGate.State.Resolving, gate.state.value)
        val job = launch { gate.resolve("user-1") }
        runCurrent()
        assertEquals(MapboxTokenGate.State.Resolving, gate.state.value)

        result.complete(true)
        job.join()

        assertEquals(MapboxTokenGate.State.Ready, gate.state.value)
    }

    @Test
    fun testGateMovesFromResolvingToUnavailableWhenResolverFails() = runTest {
        val gate = MapboxTokenGate(tokenAlreadyApplied = false) { false }

        assertEquals(MapboxTokenGate.State.Resolving, gate.state.value)
        gate.resolve(null)

        assertEquals(MapboxTokenGate.State.Unavailable, gate.state.value)
    }

    @Test
    fun testGatePassesUniqueIdToResolver() = runTest {
        val requestedIds = mutableListOf<String?>()
        val gate = MapboxTokenGate(tokenAlreadyApplied = false) { requestedIds += it; true }

        gate.resolve("user-42")

        assertEquals(listOf<String?>("user-42"), requestedIds)
    }

    @Test
    fun testGateResolvesOnlyOnceAfterSettling() = runTest {
        var calls = 0
        val gate = MapboxTokenGate(tokenAlreadyApplied = false) { calls++; false }

        gate.resolve("user-1")
        gate.resolve("user-1")

        assertEquals(1, calls)
        assertEquals(MapboxTokenGate.State.Unavailable, gate.state.value)
    }

    @Test
    fun testConcurrentResolveCallsRunResolverOnce() = runTest {
        val result = CompletableDeferred<Boolean>()
        var calls = 0
        val gate = MapboxTokenGate(tokenAlreadyApplied = false) { calls++; result.await() }

        val first = launch { gate.resolve("user-1") }
        val second = launch { gate.resolve("user-1") }
        runCurrent()
        assertEquals(1, calls)

        result.complete(true)
        first.join()
        second.join()

        assertEquals(1, calls)
        assertEquals(MapboxTokenGate.State.Ready, gate.state.value)
    }
}
