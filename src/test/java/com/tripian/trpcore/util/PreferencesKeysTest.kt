package com.tripian.trpcore.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the per-identity saved-trip-hash key builder used by [Preferences.Keys].
 */
class PreferencesKeysTest {

    @Test
    fun savedTripHashForBuildsKeyScopedToTheUniqueId() {
        assertEquals(
            "saved_trip_hash.user-a",
            Preferences.Keys.savedTripHashFor("user-a")
        )
    }

    @Test
    fun savedTripHashForDiffersAcrossIdentities() {
        val keyA = Preferences.Keys.savedTripHashFor("user-a")
        val keyB = Preferences.Keys.savedTripHashFor("user-b")

        assertEquals(false, keyA == keyB)
    }
}
