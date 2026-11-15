package com.farmdashboard.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * How readings.json is stored. Items are named after farm items ("item": "Cow").
 * Times are text like 2026-11-09T15:00 and status words are capitals, as the plan says.
 * Private fields with getters and setters and a no-argument constructor, so Gson can fill them.
 */
public class ReadingsData {

    private List<Reading> readings = new ArrayList<>();

    public ReadingsData() {
    }

    public List<Reading> getReadings() {
        return readings == null ? new ArrayList<>() : readings;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }

    /** The reading for a farm item name (any case), or empty. */
    public Optional<Reading> find(String itemName) {
        for (Reading reading : getReadings()) {
            if (reading.getItem() != null && reading.getItem().equalsIgnoreCase(itemName)) {
                return Optional.of(reading);
            }
        }
        return Optional.empty();
    }

    /** Keeps a reading attached to its item after a rename. True if a reading was changed. */
    public boolean rename(String oldName, String newName) {
        Optional<Reading> reading = find(oldName);
        reading.ifPresent(r -> r.setItem(newName));
        return reading.isPresent();
    }

    public static class Reading {
        private String item;
        private String updated;
        private String status;
        private String label;          // what percent measures: "full", "battery"
        private Integer percent;       // null when the item has no percentage
        private String note;
        private List<Zone> zones;      // soil moisture, crops only
        private List<String> growthBehind;

        public Reading() {
        }

        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }

        public LocalDateTime getUpdated() {
            return updated == null ? null : LocalDateTime.parse(updated);
        }

        public void setUpdated(LocalDateTime time) {
            this.updated = time.truncatedTo(ChronoUnit.SECONDS).toString();
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public Integer getPercent() { return percent; }
        public void setPercent(Integer percent) { this.percent = percent; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public List<Zone> getZones() { return zones == null ? List.of() : zones; }
        public void setZones(List<Zone> zones) { this.zones = zones; }

        public List<String> getGrowthBehind() {
            return growthBehind == null ? List.of() : growthBehind;
        }
        public void setGrowthBehind(List<String> growthBehind) { this.growthBehind = growthBehind; }
    }

    public static class Zone {
        private String name;
        private int moisture;

        public Zone() {
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getMoisture() { return moisture; }
        public void setMoisture(int moisture) { this.moisture = moisture; }
    }
}
