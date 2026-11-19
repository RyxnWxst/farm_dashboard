package com.farmdashboard.data;

import java.util.ArrayList;
import java.util.List;

/** How drone.json is stored: the flight settings and a log of every flight flown. */
public class DroneData {

    private double speed;        // px/s
    private double hoverSeconds; // how long a visit hovers over its target
    private double laneSpacing;  // px between scan lanes
    private double margin;       // px a scan keeps away from the map's edges
    private List<FlightLogEntry> flightLog = new ArrayList<>();

    public DroneData() {
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getHoverSeconds() {
        return hoverSeconds;
    }

    public void setHoverSeconds(double hoverSeconds) {
        this.hoverSeconds = hoverSeconds;
    }

    public double getLaneSpacing() {
        return laneSpacing;
    }

    public void setLaneSpacing(double laneSpacing) {
        this.laneSpacing = laneSpacing;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public List<FlightLogEntry> getFlightLog() {
        return flightLog;
    }

    public void setFlightLog(List<FlightLogEntry> flightLog) {
        this.flightLog = flightLog;
    }

    /** One flight in the log: a visit to a named item, or a scan of the whole farm. */
    public static class FlightLogEntry {
        public static final String VISIT = "VISIT";
        public static final String SCAN = "SCAN";

        private String type;
        private String target;          // the visited item's name; null for a scan
        private String startTime;       // ISO-8601, e.g. "2026-11-15T14:30:00"
        private double durationSeconds;

        public FlightLogEntry() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public double getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(double durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
