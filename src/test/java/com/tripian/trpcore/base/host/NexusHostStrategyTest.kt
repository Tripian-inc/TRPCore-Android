package com.tripian.trpcore.base.host

import android.content.SharedPreferences
import com.tripian.trpcore.util.Preferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Backs a mocked [Preferences] with an in-memory map so [NexusHostStrategy]'s
 * saved-trip-hash scoping can be exercised without a real Android Context.
 */
private class FakePreferences {
    private val store = mutableMapOf<String, String>()
    val shared: SharedPreferences = mockk {
        every { contains(any()) } answers { store.containsKey(firstArg()) }
    }
    val preferences: Preferences = mockk {
        every { pref } returns shared
        every { getString(any()) } answers { store[firstArg()] }
        every { setString(any(), any()) } answers { store[firstArg()] = secondArg(); Unit }
        every { deleteKey(any()) } answers { store.remove(firstArg<String>()); Unit }
    }

    fun seed(key: String, value: String) {
        store[key] = value
    }

    fun raw(key: String): String? = store[key]
}

class NexusHostStrategyTest {

    private val strategy = NexusHostStrategy()

    @Test
    fun storedTripHashIsNullWithoutAnActiveIdentity() {
        val fake = FakePreferences()

        assertNull(strategy.storedTripHash(fake.preferences))
    }

    @Test
    fun onTimelineCreatedIsNoOpWithoutAnActiveIdentity() {
        val fake = FakePreferences()

        strategy.onTimelineCreated(fake.preferences, "hash-1")

        assertNull(strategy.storedTripHash(fake.preferences))
    }

    @Test
    fun twoIdentitiesStoreIndependentTripHashes() {
        val fake = FakePreferences()
        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a")
        strategy.onTimelineCreated(fake.preferences, "hash-a")

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-b")
        strategy.onTimelineCreated(fake.preferences, "hash-b")

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a")
        assertEquals("hash-a", strategy.storedTripHash(fake.preferences))

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-b")
        assertEquals("hash-b", strategy.storedTripHash(fake.preferences))
    }

    @Test
    fun clearStoredTripHashOnlyAffectsTheActiveIdentity() {
        val fake = FakePreferences()
        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a")
        strategy.onTimelineCreated(fake.preferences, "hash-a")

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-b")
        strategy.onTimelineCreated(fake.preferences, "hash-b")

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a")
        strategy.clearStoredTripHash(fake.preferences)
        assertNull(strategy.storedTripHash(fake.preferences))

        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-b")
        assertEquals("hash-b", strategy.storedTripHash(fake.preferences))
    }

    @Test
    fun legacyUnscopedKeyIsDeletedAndNeverReturned() {
        val fake = FakePreferences()
        fake.seed(Preferences.Keys.SAVED_TRIP_HASH, "legacy-hash")
        fake.seed(Preferences.Keys.ACTIVE_UNIQUE_ID, "user-a")

        assertNull(strategy.storedTripHash(fake.preferences))
        assertNull(fake.raw(Preferences.Keys.SAVED_TRIP_HASH))
    }
}
