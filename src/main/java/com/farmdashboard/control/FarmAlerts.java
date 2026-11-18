package com.farmdashboard.control;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.ReadingsData;
import com.farmdashboard.data.WeatherData;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

public class FarmAlerts {

    public enum Severity { WATCH, ACT }

    public record Alert(Severity severity, String message) { }

    static final int LOW_MOISTURE_PERCENT = 25;
    static final double UNSAFE_WIND_MPH = 20;
    static final int UNSAFE_RAIN_PERCENT = 50;

    private static final DateTimeFormatter HOUR_FORMAT =
            DateTimeFormatter.ofPattern("h a", Locale.US);

    private final ItemContainer root;

    public FarmAlerts(ItemContainer root) {
        this.root = root;
    }

    public List<Alert> getAlerts() {
        List<Alert> alerts = new ArrayList<>();
        addMissingDrone(alerts);
        addOutOfBounds(alerts);
        addLowReadings(alerts);
        addUnsafeForecast(alerts);
        return alerts;
    }

    private void addMissingDrone(List<Alert> alerts) {
        if (root.findByName("Drone") == null) {
            alerts.add(new Alert(Severity.ACT, "No drone on the farm"));
        }
    }

    private void addOutOfBounds(List<Alert> alerts) {
        collectOutOfBounds(root, alerts);
    }

    private void collectOutOfBounds(FarmComponent parent, List<Alert> alerts) {
        for (FarmComponent child : parent.getChild()) {
            if (!child.isInside(parent)) {
                alerts.add(new Alert(Severity.WATCH,
                        child.getName() + " is outside " + parent.getName()));
            }
            collectOutOfBounds(child, alerts);
        }
    }

    private void addLowReadings(List<Alert> alerts) {
        ReadingsData readings =
                FarmDataStore.getInstance().get(DataFile.READINGS, ReadingsData.class);
        if (readings == null || readings.getReadings() == null) {
            return;
        }
        for (var reading : readings.getReadings()) {

            if (root.findByName(reading.getItem()) == null) {
                continue;
            }
            var zones = reading.getZones();
            if (zones == null) {
                continue;
            }
            for (var zone : zones) {
                if (zone.getMoisture() < LOW_MOISTURE_PERCENT) {
                    alerts.add(new Alert(Severity.ACT,
                            "Low soil moisture in the " + reading.getItem()
                            + "'s " + zone.getName() + " (" + zone.getMoisture() + "%)"));
                }
            }
        }
    }

    private void addUnsafeForecast(List<Alert> alerts) {
        WeatherData weather =
                FarmDataStore.getInstance().get(DataFile.WEATHER, WeatherData.class);
        if (weather == null || weather.getForecast() == null) {
            return;
        }

        for (var entry : weather.getForecast()) {
            boolean unsafe = entry.getWindMph() > UNSAFE_WIND_MPH
                    || entry.getRainPercent() >= UNSAFE_RAIN_PERCENT;
            if (unsafe) {
                alerts.add(new Alert(Severity.ACT,
                        "Unsafe flying forecast for " + formatTime(entry.getTime())));
                return;
            }
        }
    }

    private static String formatTime(String isoText) {

        return LocalDateTime.parse(isoText).format(HOUR_FORMAT);
    }
}