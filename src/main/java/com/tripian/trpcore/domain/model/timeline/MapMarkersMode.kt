package com.tripian.trpcore.domain.model.timeline

/**
 * Defines the map markers display mode for multi-city days.
 * Used to switch between city markers overview and step markers detail view.
 */
enum class MapMarkersMode {
    /**
     * City markers mode - shows city icons + selected step marker only.
     * Used when zoom level is <= MULTI_CITY_ZOOM_THRESHOLD (12.0).
     */
    CITY_MARKERS,

    /**
     * Step markers mode - shows all step markers.
     * Used when zoom level is > MULTI_CITY_ZOOM_THRESHOLD (12.0)
     * or for single-city days.
     */
    STEP_MARKERS
}
