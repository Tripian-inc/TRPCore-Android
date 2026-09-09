package com.tripian.trpcore.domain.model.timeline

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegment

/** True when the coordinate is absent or the (0, 0) placeholder. */
fun Coordinate?.isMissingOrZero(): Boolean = this == null || (lat == 0.0 && lng == 0.0)

/**
 * Whether an activity segment has no real-world coordinate: either flagged
 * `isNoLocation` by the client that created it, or carrying no usable coordinate.
 */
fun TimelineSegment.hasNoLocation(): Boolean =
    additionalData?.isNoLocation == true ||
        (additionalData?.coordinate ?: coordinate).isMissingOrZero()
