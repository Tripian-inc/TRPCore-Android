package com.tripian.trpcore.repository

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal in-memory [SharedPreferences] used by JVM unit tests; only the
 * accessors [MiscRepository] and [Preferences] exercise are implemented.
 */
class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        defValues

    override fun getInt(key: String?, defValue: Int): Int =
        values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor {
            key?.let { pending[it] = values }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { toRemove.add(it) }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        @Synchronized
        override fun commit(): Boolean {
            apply()
            return true
        }

        @Synchronized
        override fun apply() {
            if (clearAll) values.clear()
            toRemove.forEach { values.remove(it) }
            values.putAll(pending)
            pending.clear()
            toRemove.clear()
            clearAll = false
        }
    }
}

/** A [ContextWrapper] with a null base context that only backs [getSharedPreferences]. */
class FakeContext : ContextWrapper(null) {
    val preferences = InMemorySharedPreferences()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
}

/**
 * In-memory [SharedPreferences] whose [gatedRead]-th read of [gatedKey] blocks
 * until [release] is called (or a safety timeout elapses) and records the
 * reading thread. Other reads pass through.
 */
class GatedSharedPreferences(
    private val gatedKey: String,
    private val gatedRead: Int = 1,
    private val delegate: SharedPreferences = InMemorySharedPreferences()
) : SharedPreferences by delegate {
    private val readStarted = CountDownLatch(1)
    private val released = CountDownLatch(1)
    private val readCount = AtomicInteger(0)

    @Volatile
    var readerThread: Thread? = null
        private set

    override fun getString(key: String?, defValue: String?): String? {
        if (key == gatedKey && readCount.incrementAndGet() == gatedRead) {
            readerThread = Thread.currentThread()
            readStarted.countDown()
            released.await(GATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        return delegate.getString(key, defValue)
    }

    /** Blocks until a read of [gatedKey] has started; returns `false` on timeout. */
    fun awaitGatedRead(): Boolean = readStarted.await(GATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    fun release() = released.countDown()

    private companion object {
        const val GATE_TIMEOUT_SECONDS = 5L
    }
}

/** A [ContextWrapper] with a null base context backed by [GatedSharedPreferences]. */
class GatedContext(gatedKey: String, gatedRead: Int = 1) : ContextWrapper(null) {
    val preferences = GatedSharedPreferences(gatedKey, gatedRead)

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
}
