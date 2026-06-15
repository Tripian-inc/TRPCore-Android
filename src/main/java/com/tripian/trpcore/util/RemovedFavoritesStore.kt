package com.tripian.trpcore.util

/**
 * Persists, per tripHash, the set of base activity IDs the user removed from
 * Saved Plans. A removed favorite must not reappear as a saved plan for that
 * timeline even after the app is closed and reopened, so the suppression list
 * is stored locally and consulted when favorites are filtered.
 *
 * IDs are stored in their base form (e.g. "15423") with the "C_" prefix and any
 * provider/city suffixes stripped, so the same activity matches regardless of
 * the formatting variant it arrives in.
 */
object RemovedFavoritesStore {

    private const val KEY_PREFIX = "removed_saved_plans_"
    private const val SEPARATOR = ","

    /** Base activity IDs removed for [tripHash]. Empty when none / blank hash. */
    fun removedBaseIds(preferences: Preferences, tripHash: String): Set<String> {
        if (tripHash.isBlank()) return emptySet()
        val raw = preferences.getString(KEY_PREFIX + tripHash) ?: return emptySet()
        return raw.split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Adds [activityId]'s base form to the removed set for [tripHash]. */
    fun addRemoved(preferences: Preferences, tripHash: String, activityId: String?) {
        val baseId = baseActivityId(activityId) ?: return
        if (tripHash.isBlank()) return
        val current = removedBaseIds(preferences, tripHash).toMutableSet()
        if (current.add(baseId)) {
            preferences.setString(KEY_PREFIX + tripHash, current.joinToString(SEPARATOR))
        }
    }

    /** True when [activityId] (in any format) was removed for [tripHash]. */
    fun isRemoved(preferences: Preferences, tripHash: String, activityId: String?): Boolean {
        val baseId = baseActivityId(activityId) ?: return false
        return baseId in removedBaseIds(preferences, tripHash)
    }

    /**
     * Strips the "C_" prefix and any provider/city suffixes from an activity ID,
     * leaving the base numeric ID. "C_15423_15_28" and "15423" both → "15423".
     */
    fun baseActivityId(activityId: String?): String? {
        if (activityId.isNullOrBlank()) return null
        val withoutPrefix = if (activityId.startsWith("C_")) {
            activityId.removePrefix("C_")
        } else {
            activityId
        }
        return withoutPrefix.split("_").firstOrNull()?.takeIf { it.isNotBlank() }
    }
}
