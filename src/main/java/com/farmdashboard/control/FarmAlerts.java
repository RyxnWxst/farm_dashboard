package com.farmdashboard.control;

import java.util.ArrayList;
import java.util.List;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

public class FarmAlerts {

    public enum Severity { WATCH, ACT }

    public record Alert(Severity severity, String message) { }

    private final ItemContainer root;
    private final ItemReadings itemReadings;
    private final FlightConditions conditions;

    public FarmAlerts(ItemContainer root, ItemReadings itemReadings, FlightConditions conditions) {
        this.root = root;
        this.itemReadings = itemReadings;
        this.conditions = conditions;
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
        for (String message : itemReadings.lowMoistureAlerts(root)) {
            alerts.add(new Alert(Severity.ACT, message));
        }
    }

    private void addUnsafeForecast(List<Alert> alerts) {
        conditions.firstUnsafe().ifPresent(entry -> alerts.add(new Alert(Severity.ACT,
                "Unsafe flying forecast for " + entry.getLabel())));
    }
}