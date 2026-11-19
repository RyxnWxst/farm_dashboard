package com.farmdashboard.control;

import java.util.ArrayList;
import java.util.List;

import com.farmdashboard.data.DroneData;
import com.farmdashboard.entity.FarmComponent;

/** Turns a visit or a scan into a list of waypoints, and works out how far and how long it flies. */
public class FlightPlanner {

    // Keeps rounding from dropping the last lane when the lanes fit exactly.
    private static final double EPSILON = 1e-9;

    private final double speed;
    private final double hoverSeconds;
    private final double laneSpacing;
    private final double margin;

    public FlightPlanner(DroneData settings) {
        if (settings == null) {
            throw new IllegalArgumentException("The flight planner needs the drone settings.");
        }
        if (settings.getSpeed() <= 0) {
            throw new IllegalArgumentException("The drone's speed must be more than 0.");
        }
        if (settings.getHoverSeconds() < 0) {
            throw new IllegalArgumentException("The hover time can't be negative.");
        }
        if (settings.getLaneSpacing() <= 0) {
            throw new IllegalArgumentException("The scan lane spacing must be more than 0.");
        }
        if (settings.getMargin() < 0) {
            throw new IllegalArgumentException("The scan margin can't be negative.");
        }
        this.speed = settings.getSpeed();
        this.hoverSeconds = settings.getHoverSeconds();
        this.laneSpacing = settings.getLaneSpacing();
        this.margin = settings.getMargin();
    }

    /** Drone center, then the target's center with a hover, then back to the drone center. */
    public List<Waypoint> planVisit(FarmComponent drone, FarmComponent target) {
        requireNode(drone, "drone");
        requireNode(target, "target");
        return List.of(
                new Waypoint(drone.getCenterX(), drone.getCenterY()),
                new Waypoint(target.getCenterX(), target.getCenterY(), hoverSeconds),
                new Waypoint(drone.getCenterX(), drone.getCenterY()));
    }

    /**
     * Drone center, then lanes laneSpacing apart from y = margin down to y = width - margin,
     * each running from x = margin to x = length - margin and alternating direction, starting
     * left to right, then back to the drone center.
     */
    public List<Waypoint> planScan(FarmComponent drone, FarmComponent root) {
        requireNode(drone, "drone");
        requireNode(root, "farm");
        double left = margin;
        double right = root.getLength() - margin;
        double top = margin;
        double bottom = root.getWidth() - margin;
        if (right < left || bottom < top) {
            throw new IllegalArgumentException("The farm is too small to scan with a "
                    + margin + " px margin.");
        }

        List<Waypoint> route = new ArrayList<>();
        Waypoint home = new Waypoint(drone.getCenterX(), drone.getCenterY());
        route.add(home);
        // Count the lanes rather than adding laneSpacing in a loop, so error can't build up.
        int lanes = (int) Math.floor((bottom - top) / laneSpacing + EPSILON) + 1;
        for (int lane = 0; lane < lanes; lane++) {
            double y = top + lane * laneSpacing;
            boolean leftToRight = lane % 2 == 0;
            route.add(new Waypoint(leftToRight ? left : right, y));
            route.add(new Waypoint(leftToRight ? right : left, y));
        }
        route.add(home);
        return List.copyOf(route);
    }

    /** Total straight-line length of the route, in pixels. */
    public static double distance(List<Waypoint> waypoints) {
        double total = 0;
        for (int i = 1; i < waypoints.size(); i++) {
            Waypoint from = waypoints.get(i - 1);
            Waypoint to = waypoints.get(i);
            total += Math.hypot(to.x() - from.x(), to.y() - from.y());
        }
        return total;
    }

    /** How long the route takes: flying time at this planner's speed plus every hover. */
    public double seconds(List<Waypoint> waypoints) {
        double hovering = 0;
        for (Waypoint waypoint : waypoints) {
            hovering += waypoint.hoverSeconds();
        }
        return distance(waypoints) / speed + hovering;
    }

    private static void requireNode(FarmComponent node, String role) {
        if (node == null) {
            throw new IllegalArgumentException("No " + role + " was given for the flight.");
        }
    }
}
