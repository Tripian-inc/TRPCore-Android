package com.tripian.trpcore.util;

/**
 * This is the main model for using {@link com.mapbox.api.directions.v5.models.RouteLeg}. LegStepper is typically used by
 *
 * <p>You can create a LegStepper instance by invoking {@code new LegStepper()} if the default configuration
 * is all you need. You can also generate constructor via including parameters of LegStepper instance</p>
 */

// TODO: degisicek
public class LegStepper {
    private com.mapbox.geojson.Point firstPoint;
    private com.mapbox.geojson.Point lastPoint;
    private int legOrder;

    /**
     * Constructs a LegStepper object with firstPoint, lastPoint and legOrder params.
     *
     * @param firstPoint
     * @param lastPoint
     * @param legOrder
     */
    public LegStepper(com.mapbox.geojson.Point firstPoint, com.mapbox.geojson.Point lastPoint, int legOrder) {
        this.firstPoint = firstPoint;
        this.lastPoint = lastPoint;
        this.legOrder = legOrder;
    }

    public com.mapbox.geojson.Point getFirstPoint() {
        return firstPoint;
    }

    public com.mapbox.geojson.Point getLastPoint() {
        return lastPoint;
    }

    public int getLegOrder() {
        return legOrder;
    }
}
