package com.farmdashboard.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

/** Loads the shipped test-farm.json and checks it against the brief's numbers. */
class TestFarmTest {

    private static final double DELTA = 0.001;

    @TempDir
    Path tempDir;

    private ItemContainer root;

    @BeforeEach
    void loadTestFarm() {
        FarmDataStore store = FarmDataStore.getInstance();
        store.useFolder(tempDir);
        root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        FarmJson.copyInto(root, store.get(DataFile.TEST_FARM, NodeData.class));
    }

    @Test
    void totalsMatchTheBrief() {
        assertEquals(177_500, root.getTotal(), DELTA);
        assertEquals(64_500, root.findByName("Barn").getTotal(), DELTA);
        assertEquals(78_500, root.findByName("Storage building").getTotal(), DELTA);
        assertEquals(22_500, root.findByName("Command center").getTotal(), DELTA);
    }

    @Test
    void theFarmHasTheBriefsStructure() {
        assertEquals(4, root.getChild().size());
        assertEquals("Livestock area", root.findByName("Cow").getParent().getName());
        assertEquals("Barn", root.findByName("Milk storage").getParent().getName());
        assertEquals("Storage building", root.findByName("Tiller").getParent().getName());
        assertEquals("Command center", root.findByName("Drone").getParent().getName());
        assertEquals("Root", root.findByName("Soy crop").getParent().getName());
    }

    @Test
    void everyNodeLiesInsideItsParent() {
        checkInside(root);
    }

    @Test
    void noTwoSiblingsOverlap() {
        checkSiblings(root);
    }

    @Test
    void theDroneIsFoundAndCenteredAt105x80() {
        FarmComponent drone = root.findByName("drone");
        assertNotNull(drone);
        assertEquals(105, drone.getCenterX(), DELTA);
        assertEquals(80, drone.getCenterY(), DELTA);
    }

    @Test
    void theCowCenterIsTheVisitTarget() {
        FarmComponent cow = root.findByName("Cow");
        assertEquals(570, cow.getCenterX(), DELTA);
        assertEquals(360, cow.getCenterY(), DELTA);
    }

    private static void checkInside(FarmComponent node) {
        for (FarmComponent child : node.getChild()) {
            assertTrue(child.isInside(node), child.getName() + " should be inside " + node.getName());
            checkInside(child);
        }
    }

    private static void checkSiblings(FarmComponent node) {
        List<FarmComponent> kids = node.getChild();
        for (int i = 0; i < kids.size(); i++) {
            for (int j = i + 1; j < kids.size(); j++) {
                assertFalse(overlap(kids.get(i), kids.get(j)),
                        kids.get(i).getName() + " overlaps " + kids.get(j).getName());
            }
            checkSiblings(kids.get(i));
        }
    }

    private static boolean overlap(FarmComponent a, FarmComponent b) {
        return a.getLocationX() < b.getLocationX() + b.getLength()
                && b.getLocationX() < a.getLocationX() + a.getLength()
                && a.getLocationY() < b.getLocationY() + b.getWidth()
                && b.getLocationY() < a.getLocationY() + a.getWidth();
    }
}
