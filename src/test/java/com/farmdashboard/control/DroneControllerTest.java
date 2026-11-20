package com.farmdashboard.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.control.FlightConditions.Rating;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.data.ReadingsData;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

/** DroneController on the shipped test farm, drone.json, weather.json and readings.json. */
class DroneControllerTest {

    private static final double DELTA = 0.001;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 10, 30);
    private static final LocalDateTime OLD = LocalDateTime.of(2026, 11, 9, 15, 0);

    @TempDir
    Path tempDir;

    private FarmDataStore store;
    private ItemContainer root;
    private FarmComponent drone;
    private DroneController controller;

    @BeforeEach
    void loadTestFarm() {
        store = FarmDataStore.getInstance();
        store.useFolder(tempDir); // the Singleton lives across tests, so start each one fresh
        root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        FarmJson.copyInto(root, store.get(DataFile.TEST_FARM, NodeData.class));
        drone = root.findByName("Drone");
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        controller = new DroneController(new FlightConditions(clock), new ItemReadings(() -> NOW));
    }

    @Test
    void theDroneIsFoundWhenItsNameIsLowerCase() {
        drone.setName("drone");
        assertSame(drone, controller.findDrone(root).orElseThrow());
    }

    @Test
    void theDroneIsFoundInMixedCase() {
        drone.setName("dRoNe");
        assertSame(drone, controller.findDrone(root).orElseThrow());
    }

    @Test
    void aContainerNamedDroneIsNotTheDrone() {
        ItemContainer emptyFarm = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        emptyFarm.add(new ItemContainer("Drone", 0, 10, 10, 50, 50, 1));
        assertTrue(controller.findDrone(emptyFarm).isEmpty());
    }

    @Test
    void aFarmWithoutADroneCantFly() {
        ItemContainer emptyFarm = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        Item cow = new Item("Cow", 1500, 545, 345, 50, 30, 5);
        emptyFarm.add(cow);

        assertTrue(controller.findDrone(emptyFarm).isEmpty());
        IllegalStateException visit = assertThrows(IllegalStateException.class,
                () -> controller.planVisit(emptyFarm, cow));
        assertTrue(visit.getMessage().contains("no drone"));
        assertThrows(IllegalStateException.class, () -> controller.planScan(emptyFarm));
    }

    @Test
    void visitingTheCowGivesThreeWaypointsAndAbout5Point34Seconds() {
        List<Waypoint> route = controller.planVisit(root, root.findByName("Cow"));

        assertEquals(3, route.size());
        assertEquals(105, route.get(0).x(), DELTA);
        assertEquals(80, route.get(0).y(), DELTA);
        assertEquals(570, route.get(1).x(), DELTA);
        assertEquals(360, route.get(1).y(), DELTA);
        assertEquals(1, route.get(1).hoverSeconds(), DELTA);
        assertEquals(5.34, controller.seconds(route), 0.005);
    }

    @Test
    void theCowVisitPlanMatchesFlightPlanners() {
        FarmComponent cow = root.findByName("Cow");
        FlightPlanner planner = new FlightPlanner(store.get(DataFile.DRONE, DroneData.class));

        List<Waypoint> expected = planner.planVisit(drone, cow);
        List<Waypoint> actual = controller.planVisit(root, cow);

        assertEquals(expected, actual);
        assertEquals(planner.seconds(expected), controller.seconds(actual), DELTA);
    }

    @Test
    void aScanGives14WaypointsInAbout21Seconds() {
        List<Waypoint> route = controller.planScan(root);

        assertEquals(14, route.size());
        assertEquals(20.94, controller.seconds(route), 0.005);
    }

    @Test
    void rootTheDroneAndNothingCantBeVisited() {
        assertThrows(IllegalArgumentException.class, () -> controller.planVisit(root, root));
        assertThrows(IllegalArgumentException.class, () -> controller.planVisit(root, drone));
        assertThrows(IllegalArgumentException.class, () -> controller.planVisit(root, null));
    }

    @Test
    void anItemThatIsNotOnTheFarmCantBeVisited() {
        Item stray = new Item("Stray", 0, 10, 10, 10, 10, 1);
        assertThrows(IllegalArgumentException.class, () -> controller.planVisit(root, stray));
    }

    @Test
    void speedComesFromTheDefaultDroneFile() {
        assertEquals(250, controller.speed(), DELTA);
    }

    @Test
    void aZeroSpeedInDroneJsonIsRefused() {
        store.get(DataFile.DRONE, DroneData.class).setSpeed(0);
        assertThrows(IllegalArgumentException.class, () -> controller.speed());
    }

    @Test
    void conditionsNowPassesThroughTheCurrentRating() {
        assertEquals(Rating.CAUTION, controller.conditionsNow()); // 12 mph, 10% rain
    }

    @Test
    void eachFinishedFlightAddsExactlyOneLogRow() {
        int before = flightLogSize();

        controller.finishFlight(root, FlightLogEntry.VISIT, root.findByName("Cow"), 5.34);
        assertEquals(before + 1, flightLogSize());

        controller.finishFlight(root, FlightLogEntry.SCAN, null, 20.94);
        assertEquals(before + 2, flightLogSize());

        store.useFolder(tempDir); // the rows were saved, not just added in memory
        assertEquals(before + 2, flightLogSize());
    }

    @Test
    void finishingAVisitLogsItAndRefreshesTheTargetAndItsContents() {
        FarmComponent barn = root.findByName("Barn");

        int refreshed = controller.finishFlight(root, FlightLogEntry.VISIT, barn, 6.2);

        assertEquals(2, refreshed); // Cow and Milk storage are inside the Barn
        FlightLogEntry logged = new FlightConditions().lastFlight().orElseThrow();
        assertEquals(FlightLogEntry.VISIT, logged.getType());
        assertEquals("Barn", logged.getTarget());
        assertEquals("2026-11-16T10:30:00", logged.getStartTime());
        assertEquals(6.2, logged.getDurationSeconds(), DELTA);

        ReadingsData data = store.get(DataFile.READINGS, ReadingsData.class);
        assertEquals(NOW, data.find("Cow").orElseThrow().getUpdated());
        assertEquals(NOW, data.find("Milk storage").orElseThrow().getUpdated());
        assertEquals(OLD, data.find("Tractor").orElseThrow().getUpdated());
        assertEquals(OLD, data.find("Soy crop").orElseThrow().getUpdated());
    }

    @Test
    void finishingAScanLogsItAndRefreshesEveryReading() {
        int refreshed = controller.finishFlight(root, FlightLogEntry.SCAN, null, 20.94);

        assertEquals(6, refreshed);
        FlightLogEntry logged = new FlightConditions().lastFlight().orElseThrow();
        assertEquals(FlightLogEntry.SCAN, logged.getType());
        assertNull(logged.getTarget());
        for (ReadingsData.Reading reading : store.get(DataFile.READINGS, ReadingsData.class).getReadings()) {
            assertEquals(NOW, reading.getUpdated(), reading.getItem());
        }
    }

    @Test
    void aBadFlightIsRefusedBeforeAnythingIsLoggedOrRefreshed() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.finishFlight(root, FlightLogEntry.VISIT, null, 5));
        assertThrows(IllegalArgumentException.class,
                () -> controller.finishFlight(root, "HOVER", null, 5));

        assertTrue(new FlightConditions().lastFlight().isEmpty());
        assertEquals(OLD, store.get(DataFile.READINGS, ReadingsData.class)
                .find("Cow").orElseThrow().getUpdated());
    }

    @Test
    void planningNeverMovesTheDroneItem() {
        controller.planVisit(root, root.findByName("Cow"));
        assertDroneAtHome();

        controller.planScan(root);
        assertDroneAtHome();
    }

    @Test
    void finishingAFlightNeverMovesTheDroneItem() {
        controller.finishFlight(root, FlightLogEntry.VISIT, root.findByName("Cow"), 5.34);
        assertDroneAtHome();

        controller.finishFlight(root, FlightLogEntry.SCAN, null, 20.94);
        assertDroneAtHome();
    }

    private void assertDroneAtHome() {
        assertEquals(85, drone.getLocationX(), DELTA);
        assertEquals(60, drone.getLocationY(), DELTA);
        assertEquals(105, drone.getCenterX(), DELTA);
        assertEquals(80, drone.getCenterY(), DELTA);
    }

    private int flightLogSize() {
        return store.get(DataFile.DRONE, DroneData.class).getFlightLog().size();
    }
}
