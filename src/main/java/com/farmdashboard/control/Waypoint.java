package com.farmdashboard.control;

/** A point on the drone's route, in map pixels, and how long the drone hovers on arriving there. */
public record Waypoint(double x, double y, double hoverSeconds) {

    public Waypoint {
        if (hoverSeconds < 0) {
            throw new IllegalArgumentException("Hover time can't be negative.");
        }
    }

    /** A waypoint the drone flies straight through without hovering. */
    public Waypoint(double x, double y) {
        this(x, y, 0);
    }
}
