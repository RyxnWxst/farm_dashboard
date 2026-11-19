package com.farmdashboard.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

/**
 * Checks visit and scan routes on the AGENTS.md test farm, flown with the default drone.json
 * settings, against the expected numbers listed there.
 */
class FlightPlannerTest {

    private static final double DELTA = 0.001;

    @TempDir
    Path tempDir;

    private ItemContainer root;
    private Item drone;
    private Item cow;
    private FlightPlanner planner;

    @BeforeEach
    void buildTestFarmAndLoadDefaultSettings() {
        FarmDataStore store = FarmDataStore.getInstance();
        store.useFolder(tempDir); // the Singleton lives across tests, so start each one fresh
        planner = new FlightPlanner(store.get(DataFile.DRONE, DroneData.class));

        //                                       name                 price    x    y    L    W   H
        root = new ItemContainer("Root",                                  0,   0,   0, 800, 600,  0);
        ItemContainer barn = new ItemContainer("Barn",                50000, 500, 300, 270, 260, 30);
        ItemContainer livestockArea = new ItemContainer("Livestock area", 5000, 520, 320, 140, 110, 10);
        cow = new Item("Cow",                                          1500, 545, 345,  50,  30,  5);
        Item milkStorage = new Item("Milk storage",                    8000, 520, 460, 120,  70, 12);
        ItemContainer storage = new ItemContainer("Storage building", 30000,  30, 330, 230, 150, 25);
        Item tractor = new Item("Tractor",                            45000,  50, 360,  80,  50, 10);
        Item tiller = new Item("Tiller",                               3500, 150, 360,  70,  40,  4);
        ItemContainer command = new ItemContainer("Command center",   20000,  30,  30, 160, 110, 15);
        drone = new Item("Drone",                                      2500,  85,  60,  40,  40,  1);
        Item soyCrop = new Item("Soy crop",                           12000, 230,  30, 540, 220,  3);

        root.add(barn);
        barn.add(livestockArea);
        livestockArea.add(cow);
        barn.add(milkStorage);
        root.add(storage);
        storage.add(tractor);
        storage.add(tiller);
        root.add(command);
        command.add(drone);
        root.add(soyCrop);
    }

    @Test
    void theDefaultSettingsAreLoaded() {
        DroneData settings = FarmDataStore.getInstance().get(DataFile.DRONE, DroneData.class);
        assertEquals(250, settings.getSpeed(), DELTA);
        assertEquals(1, settings.getHoverSeconds(), DELTA);
        assertEquals(100, settings.getLaneSpacing(), DELTA);
        assertEquals(50, settings.getMargin(), DELTA);
    }

    @Test
    void theDroneIsFoundByNameAndItsCenterIsAt105And80() {
        assertEquals(drone, root.findByName("drone"));
        assertEquals(105, drone.getCenterX(), DELTA);
        assertEquals(80, drone.getCenterY(), DELTA);
    }

    @Test
    void aVisitToTheCowFliesThereHoversOneSecondAndComesBack() {
        List<Waypoint> route = planner.planVisit(drone, cow);

        assertEquals(3, route.size());
        assertWaypoint(route.get(0), 105, 80, 0);
        assertWaypoint(route.get(1), 570, 360, 1);
        assertWaypoint(route.get(2), 105, 80, 0);
    }

    @Test
    void visitingTheCowTakesAbout5Point34Seconds() {
        List<Waypoint> route = planner.planVisit(drone, cow);

        double oneWay = Math.hypot(570 - 105, 360 - 80);
        assertEquals(2 * oneWay, FlightPlanner.distance(route), DELTA);
        assertEquals(2 * oneWay / 250 + 1, planner.seconds(route), DELTA);
        assertEquals(5.34, planner.seconds(route), 0.005);
    }

    @Test
    void aScanHas14WaypointsAndStartsAndEndsAtTheDrone() {
        List<Waypoint> route = planner.planScan(drone, root);

        assertEquals(14, route.size());
        assertWaypoint(route.get(0), 105, 80, 0);
        assertWaypoint(route.get(13), 105, 80, 0);
    }

    @Test
    void aScanSweepsSixAlternatingLanes() {
        List<Waypoint> route = planner.planScan(drone, root);

        double[] laneYs = {50, 150, 250, 350, 450, 550};
        for (int lane = 0; lane < laneYs.length; lane++) {
            boolean leftToRight = lane % 2 == 0;
            assertWaypoint(route.get(1 + 2 * lane), leftToRight ? 50 : 750, laneYs[lane], 0);
            assertWaypoint(route.get(2 + 2 * lane), leftToRight ? 750 : 50, laneYs[lane], 0);
        }
    }

    @Test
    void everyScanWaypointStaysInsideTheMap() {
        for (Waypoint waypoint : planner.planScan(drone, root)) {
            assertTrue(waypoint.x() >= 0 && waypoint.x() <= 800, "x out of the map: " + waypoint);
            assertTrue(waypoint.y() >= 0 && waypoint.y() <= 600, "y out of the map: " + waypoint);
        }
    }

    @Test
    void aScanCoversAbout5236PixelsInAbout20Point9Seconds() {
        List<Waypoint> route = planner.planScan(drone, root);

        double expected = Math.hypot(55, 30) + 6 * 700 + 5 * 100 + Math.hypot(55, 470);
        assertEquals(expected, FlightPlanner.distance(route), DELTA);
        assertEquals(5236, FlightPlanner.distance(route), 0.5);
        assertEquals(expected / 250, planner.seconds(route), DELTA);
        assertEquals(20.9, planner.seconds(route), 0.05);
    }

    @Test
    void aScanStopsAtTheLastLaneThatFitsInsideTheMargin() {
        FlightPlanner wideLanes = new FlightPlanner(settings(250, 1, 120, 50));
        List<Waypoint> route = wideLanes.planScan(drone, root);

        // Lanes at y = 50, 170, 290, 410, 530; the next one (650) would pass y = 550.
        assertEquals(2 + 2 * 5, route.size());
        assertEquals(530, route.get(route.size() - 2).y(), DELTA);
    }

    @Test
    void aRouteWithNoMovementTakesOnlyItsHoverTime() {
        List<Waypoint> route = List.of(new Waypoint(10, 10), new Waypoint(10, 10, 2));

        assertEquals(0, FlightPlanner.distance(route), DELTA);
        assertEquals(2, planner.seconds(route), DELTA);
    }

    @Test
    void badSettingsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanner(settings(0, 1, 100, 50)));
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanner(settings(250, -1, 100, 50)));
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanner(settings(250, 1, 0, 50)));
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanner(settings(250, 1, 100, -1)));
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanner(null));
    }

    @Test
    void aMissingDroneOrTargetIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> planner.planVisit(null, cow));
        assertThrows(IllegalArgumentException.class, () -> planner.planVisit(drone, null));
        assertThrows(IllegalArgumentException.class, () -> planner.planScan(drone, null));
    }

    @Test
    void aFarmSmallerThanTheMarginsCantBeScanned() {
        ItemContainer tiny = new ItemContainer("Root", 0, 0, 0, 80, 80, 0);
        assertThrows(IllegalArgumentException.class, () -> planner.planScan(drone, tiny));
    }

    private static void assertWaypoint(Waypoint waypoint, double x, double y, double hover) {
        assertEquals(x, waypoint.x(), DELTA);
        assertEquals(y, waypoint.y(), DELTA);
        assertEquals(hover, waypoint.hoverSeconds(), DELTA);
    }

    // Only for the edge-case tests that need settings other than the defaults.
    private static DroneData settings(double speed, double hover, double laneSpacing, double margin) {
        DroneData settings = new DroneData();
        settings.setSpeed(speed);
        settings.setHoverSeconds(hover);
        settings.setLaneSpacing(laneSpacing);
        settings.setMargin(margin);
        return settings;
    }
}
