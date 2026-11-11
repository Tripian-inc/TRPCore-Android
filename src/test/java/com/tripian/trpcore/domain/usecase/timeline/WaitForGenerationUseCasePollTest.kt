package com.tripian.trpcore.domain.usecase.timeline

import com.tripian.one.api.timeline.model.Timeline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pins [WaitForGenerationUseCase.poll]'s retry/termination contract under virtual time:
 * attempt budget, per-attempt progress callback, failure-count abort and 4xx fast-abort.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaitForGenerationUseCasePollTest {

    private fun timeline() = Timeline()

    private fun httpException(code: Int): HttpException {
        val body = "".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    private fun paramsOf(
        maxRetries: Int = 15,
        maxFailures: Int = Int.MAX_VALUE,
        isGenerated: ((Timeline) -> Boolean)? = { false },
        onAttemptCompleted: (suspend (Int) -> Unit)? = null
    ) = WaitForGenerationUseCase.Params(
        tripHash = "trip-1",
        maxRetries = maxRetries,
        intervalMs = 2000,
        initialDelayMs = 1000,
        maxFailures = maxFailures,
        isGenerated = isGenerated,
        onAttemptCompleted = onAttemptCompleted
    )

    @Test
    fun `poll makes exactly maxRetries fetch attempts when generation never completes`() = runTest {
        var calls = 0
        val fetch: suspend () -> Timeline = { calls++; timeline() }

        WaitForGenerationUseCase.poll(paramsOf(maxRetries = 60), fetch)

        assertEquals(60, calls)
    }

    @Test
    fun `poll stops early and returns the generated timeline once isGenerated is true`() = runTest {
        var calls = 0
        val generatedAt = 5
        val generatedTimeline = timeline()
        val fetch: suspend () -> Timeline = {
            calls++
            if (calls == generatedAt) generatedTimeline else timeline()
        }

        val result = WaitForGenerationUseCase.poll(
            paramsOf(maxRetries = 60, isGenerated = { it === generatedTimeline }),
            fetch
        )

        assertEquals(generatedAt, calls)
        assertSame(generatedTimeline, result)
    }

    @Test
    fun `poll fires onAttemptCompleted for every non-terminal attempt but not for the terminal one`() = runTest {
        val completedAttempts = mutableListOf<Int>()
        var calls = 0
        val generatedAt = 5
        val generatedTimeline = timeline()
        val fetch: suspend () -> Timeline = {
            calls++
            if (calls == generatedAt) generatedTimeline else timeline()
        }

        WaitForGenerationUseCase.poll(
            paramsOf(
                maxRetries = 60,
                isGenerated = { it === generatedTimeline },
                onAttemptCompleted = { attempt -> completedAttempts += attempt }
            ),
            fetch
        )

        assertEquals(listOf(1, 2, 3, 4), completedAttempts)
    }

    @Test
    fun `poll fires onAttemptCompleted for every attempt when the budget is exhausted`() = runTest {
        val completedAttempts = mutableListOf<Int>()
        val fetch: suspend () -> Timeline = { timeline() }

        WaitForGenerationUseCase.poll(
            paramsOf(maxRetries = 5, onAttemptCompleted = { attempt -> completedAttempts += attempt }),
            fetch
        )

        assertEquals(listOf(1, 2, 3, 4, 5), completedAttempts)
    }

    @Test
    fun `poll returns the last successfully fetched timeline when the budget is exhausted`() = runTest {
        var calls = 0
        val lastTimeline = timeline()
        val fetch: suspend () -> Timeline = {
            calls++
            if (calls == 3) lastTimeline else timeline()
        }

        val result = WaitForGenerationUseCase.poll(paramsOf(maxRetries = 3), fetch)

        assertSame(lastTimeline, result)
    }

    @Test
    fun `poll aborts after maxFailures non-4xx failures and rethrows the last one`() = runTest {
        var calls = 0
        val fetch: suspend () -> Timeline = {
            calls++
            throw RuntimeException("boom-$calls")
        }

        val error = try {
            WaitForGenerationUseCase.poll(paramsOf(maxRetries = 60, maxFailures = 3), fetch)
            null
        } catch (e: RuntimeException) {
            e
        }

        assertEquals("boom-3", error?.message)
        assertEquals(3, calls)
    }

    @Test
    fun `poll tolerates failures under the failure budget and keeps polling`() = runTest {
        var calls = 0
        val fetch: suspend () -> Timeline = {
            calls++
            if (calls <= 2) throw RuntimeException("transient") else timeline()
        }

        val result = WaitForGenerationUseCase.poll(paramsOf(maxRetries = 10, maxFailures = 3), fetch)

        assertEquals(10, calls)
        assertEquals(timeline()::class, result::class)
    }

    @Test
    fun `poll rethrows a 4xx HttpException immediately without counting toward maxFailures`() = runTest {
        var calls = 0
        val fetch: suspend () -> Timeline = {
            calls++
            throw httpException(404)
        }

        val error = try {
            WaitForGenerationUseCase.poll(paramsOf(maxRetries = 60, maxFailures = 10), fetch)
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals(404, error?.code())
        assertEquals(1, calls)
    }

    @Test
    fun `poll does not swallow CancellationException`() = runTest {
        val fetch: suspend () -> Timeline = { throw CancellationException("cancelled") }

        try {
            WaitForGenerationUseCase.poll(paramsOf(maxRetries = 60), fetch)
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun `poll never fires onAttemptCompleted after a 4xx abort`() = runTest {
        val completedAttempts = mutableListOf<Int>()
        val fetch: suspend () -> Timeline = { throw httpException(400) }

        try {
            WaitForGenerationUseCase.poll(
                paramsOf(maxRetries = 60, onAttemptCompleted = { attempt -> completedAttempts += attempt }),
                fetch
            )
            fail("expected HttpException")
        } catch (_: HttpException) {
        }

        assertTrue(completedAttempts.isEmpty())
    }

    @Test
    fun `poll uses the default all-itinerary-generated rule when isGenerated is null`() = runTest {
        /** No tripProfile/plans means the default rule treats the timeline as generated. */
        val emptyTimeline = Timeline()
        val result = WaitForGenerationUseCase.poll(paramsOf(maxRetries = 1, isGenerated = null), { emptyTimeline })

        assertSame(emptyTimeline, result)
    }
}
