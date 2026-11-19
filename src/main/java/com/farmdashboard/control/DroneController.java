package com.farmdashboard.control;

import java.util.List;
import java.util.Optional;

import com.farmdashboard.control.FlightConditions.Rating;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.entity.FarmComponent;

/**
 * What the boundary calls for the visit-item and scan-farm use cases: finds the drone, plans the
 * route, reports flight conditions, and logs the flight and refreshes readings once it lands.
 * Only the screen's sprite moves during a flight; the Drone item's location is never changed here.
 */
public class DroneController {

    private static final String DRONE_NAME = "Drone";

    private final FlightConditions conditions;
    private final ItemReadings readings;

    public DroneController() {
        this(new FlightConditions(), new ItemReadings());
    }

    // Tests pass in versions with fixed clocks.
    DroneController(FlightConditions conditions, ItemReadings readings) {
        this.conditions = conditions;
        this.readings = readings;
    }

    /** The item named "Drone", in any case, anywhere on the farm. Containers don't count. */
    public Optional<FarmComponent> findDrone(FarmComponent root) {
        if (root == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(findItemNamed(root, DRONE_NAME));
    }

    /** Drone center, target center with a hover, drone center. Root and the drone can't be visited. */
    public List<Waypoint> planVisit(FarmComponent root, FarmComponent target) {
        FarmComponent drone = requireDrone(root);
        if (target == null) {
            throw new IllegalArgumentException("Choose an item to visit first.");
        }
        if (target == root) {
            throw new IllegalArgumentException("Root is the whole farm; use Scan Farm to fly over it.");
        }
        if (target == drone) {
            throw new IllegalArgumentException("The drone can't visit itself.");
        }
        if (!isOnFarm(root, target)) {
            throw new IllegalArgumentException(target.getName() + " isn't on this farm.");
        }
        return planner().planVisit(drone, target);
    }

    /** The lawn-mower sweep over the whole farm, starting and ending at the drone. */
    public List<Waypoint> planScan(FarmComponent root) {
        return planner().planScan(requireDrone(root), root);
    }

    /** How long a planned route takes, so the boundary can time the animation. */
    public double seconds(List<Waypoint> route) {
        return planner().seconds(route);
    }

    /** The current flight rating; the boundary asks for confirmation before an UNSAFE launch. */
    public Rating conditionsNow() {
        return conditions.now();
    }

    /**
     * Call once the drone is back: logs the flight in drone.json, then refreshes the readings the
     * flight covered (the target and everything inside it for a visit, the whole farm for a scan).
     * Returns how many readings were refreshed.
     */
    public int finishFlight(FarmComponent root, String type, FarmComponent target, double seconds) {
        if (root == null) {
            throw new IllegalArgumentException("No farm was given for the flight.");
        }
        // logFlight checks the type, the target and the time before anything is saved.
        conditions.logFlight(type, target == null ? null : target.getName(), seconds);
        if (FlightLogEntry.VISIT.equals(type)) {
            return readings.refreshVisit(target);
        }
        return readings.refreshScan(root);
    }

    // Read on every flight so a settings change in drone.json is picked up.
    private static FlightPlanner planner() {
        return new FlightPlanner(FarmDataStore.getInstance().get(DataFile.DRONE, DroneData.class));
    }

    private FarmComponent requireDrone(FarmComponent root) {
        return findDrone(root).orElseThrow(() -> new IllegalStateException(
                "There's no drone on the farm. Add an item named \"Drone\" to fly."));
    }

    // Not findByName: that would also match a container named "Drone".
    private static FarmComponent findItemNamed(FarmComponent node, String name) {
        if (!node.isContainer() && name.equalsIgnoreCase(node.getName())) {
            return node;
        }
        for (FarmComponent child : node.getChild()) {
            FarmComponent found = findItemNamed(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean isOnFarm(FarmComponent root, FarmComponent node) {
        for (FarmComponent n = node; n != null; n = n.getParent()) {
            if (n == root) {
                return true;
            }
        }
        return false;
    }
}
