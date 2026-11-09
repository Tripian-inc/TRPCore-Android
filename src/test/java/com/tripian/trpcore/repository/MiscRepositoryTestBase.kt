package com.tripian.trpcore.repository

import android.app.Application
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tripian.trpcore.base.AppConfig
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.Preferences
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before

/**
 * Shared fixtures for [MiscRepository] coroutine/cache tests: a fresh
 * [TRPCore.core], an in-memory [Preferences] backed by [FakeContext], and
 * helpers to build v2 translation payloads and poll for background state.
 */
abstract class MiscRepositoryTestBase {

    @Before
    fun setUpTrpCoreAndArchExecutor() {
        TRPCore.core = TRPCore().apply {
            appConfig = object : AppConfig() {
                override fun tripianServiceUrl(): String = ""
                override fun apiKey(): String = ""
                override fun mapboxApiKey(): String = ""
                override fun apiVersion(): String = ""
            }
        }
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDownArchExecutor() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    fun newRepository(): MiscRepository {
        val preferences = Preferences(FakeContext())
        return MiscRepository(Application(), preferences)
    }

    /** Builds a v2 translations payload carrying a single [language]. */
    fun payload(language: String, label: String, helloValue: String): String {
        return """
            {"data":{"translations":{"$language":{"keys":{"hello":"$helloValue"}}},"langCodes":[{"value":"$language","label":"$label"}]}}
        """.trimIndent()
    }

    /** Busy-polls [condition] on the calling thread until it is true or [timeoutMs] elapses. */
    fun waitUntil(timeoutMs: Long = 2000, intervalMs: Long = 10, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                fail("condition not met within ${timeoutMs}ms")
            }
            Thread.sleep(intervalMs)
        }
    }
}
