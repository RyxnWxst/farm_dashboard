package com.farmdashboard.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.data.ReadingsData;
import com.farmdashboard.entity.ItemContainer;

/** ItemReadings against the test farm and the default readings.json, in a temp folder. */
class ItemReadingsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 10, 30);
    private static final LocalDateTime OLD = LocalDateTime.of(2026, 11, 9, 15, 0);

    @TempDir
    Path tempDir;

    private ItemContainer root;
    private ItemReadings readings;

    @BeforeEach
    void setUp() {
        FarmDataStore store = FarmDataStore.getInstance();
        store.useFolder(tempDir);
        root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        FarmJson.copyInto(root, store.get(DataFile.TEST_FARM, NodeData.class));
        readings = new ItemReadings(() -> NOW);
    }

    @Test
    void zone2IsFlaggedBelow25Percent() {
        List<String> alerts = readings.lowMoistureAlerts(root);
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("Zone 2"));
        assertTrue(alerts.get(0).contains("18%"));
    }

    @Test
    void noAlertWhenTheCropIsNoLongerOnTheFarm() {
        root.remove(root.findByName("Soy crop"));
        assertTrue(readings.lowMoistureAlerts(root).isEmpty());
    }

    @Test
    void visitingTheCowRefreshesOnlyTheCow() {
        assertEquals(1, readings.refreshVisit(root.findByName("Cow")));
        assertEquals(NOW, updated("Cow"));
        assertEquals(OLD, updated("Milk storage"));
        assertEquals(OLD, updated("Tractor"));
    }

    @Test
    void visitingTheBarnRefreshesCowAndMilkStorageButNotTractor() {
        assertEquals(2, readings.refreshVisit(root.findByName("Barn")));
        assertEquals(NOW, updated("Cow"));
        assertEquals(NOW, updated("Milk storage"));
        assertEquals(OLD, updated("Tractor"));
    }

    @Test
    void aScanRefreshesAllSixReadings() {
        assertEquals(6, readings.refreshScan(root));
        for (String name : List.of("Soy crop", "Cow", "Tractor", "Tiller", "Milk storage", "Drone")) {
            assertEquals(NOW, updated(name), name);
        }
    }

    @Test
    void readingsForItemsNoLongerOnTheFarmAreSkipped() {
        root.remove(root.findByName("Storage building"));
        assertEquals(4, readings.refreshScan(root)); // Tractor and Tiller are gone
    }

    @Test
    void refreshedTimesSurviveARestart() {
        readings.refreshScan(root);
        FarmDataStore.getInstance().useFolder(tempDir); // forget the cache, as a restart would
        assertEquals(NOW, updated("Cow"));
    }

    @Test
    void aRenameKeepsTheReadingWithItsItem() {
        ReadingsData data = FarmDataStore.getInstance().get(DataFile.READINGS, ReadingsData.class);
        assertTrue(data.rename("Cow", "Bessie"));
        assertTrue(readings.latest("Bessie").isPresent());
        assertFalse(readings.latest("Cow").isPresent());
    }

    private LocalDateTime updated(String item) {
        return readings.latest(item).orElseThrow().getUpdated();
    }
}
