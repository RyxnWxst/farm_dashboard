package com.farmdashboard.control;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.ReadingsData;
import com.farmdashboard.data.ReadingsData.Reading;
import com.farmdashboard.data.ReadingsData.Zone;
import com.farmdashboard.entity.FarmComponent;

/**
 * Control class for the drone's readings (readings.json). No JavaFX in here.
 * Alerts and Status read through it; DroneController calls refreshVisit/refreshScan after a flight.
 * Readings whose item is no longer on the farm are skipped.
 */
public class ItemReadings {

    /** Soil moisture below this percent is flagged. */
    public static final int MOISTURE_LIMIT = 25;

    private final Supplier<LocalDateTime> clock;

    public ItemReadings() {
        this(LocalDateTime::now);
    }

    /** The clock is a parameter so tests can use a fixed time. */
    public ItemReadings(Supplier<LocalDateTime> clock) {
        this.clock = clock;
    }

    /** The latest reading for a farm item name (any case), for Status. */
    public Optional<Reading> latest(String itemName) {
        return data().find(itemName);
    }

    /** One line per low soil-moisture zone of items still on the farm, for Alerts. */
    public List<String> lowMoistureAlerts(FarmComponent root) {
        List<String> alerts = new ArrayList<>();
        for (Reading reading : data().getReadings()) {
            if (root.findByName(reading.getItem()) == null) {
                continue;
            }
            for (Zone zone : reading.getZones()) {
                if (zone.getMoisture() < MOISTURE_LIMIT) {
                    alerts.add("Low soil moisture in " + reading.getItem() + "'s "
                            + zone.getName() + " (" + zone.getMoisture() + "%)");
                }
            }
        }
        return alerts;
    }

    /** After a visit: refreshes the target and everything inside it. Returns how many. */
    public int refreshVisit(FarmComponent target) {
        return refresh(r -> target.findByName(r.getItem()) != null);
    }

    /** After a scan: refreshes every reading whose item is on the farm. Returns how many. */
    public int refreshScan(FarmComponent root) {
        return refresh(r -> root.findByName(r.getItem()) != null);
    }

    private int refresh(Predicate<Reading> covered) {
        LocalDateTime now = clock.get();
        int count = 0;
        for (Reading reading : data().getReadings()) {
            if (covered.test(reading)) {
                reading.setUpdated(now);
                count++;
            }
        }
        if (count > 0) {
            FarmDataStore.getInstance().save(DataFile.READINGS); // runs the onChange listeners
        }
        return count;
    }

    private ReadingsData data() {
        return FarmDataStore.getInstance().get(DataFile.READINGS, ReadingsData.class);
    }
}
