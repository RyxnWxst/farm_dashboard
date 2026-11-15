package com.farmdashboard.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

// Checks that a farm saved with FarmJson comes back the same, in a temp folder.
class FarmJsonTest {

    private static final double DELTA = 0.001;

    @TempDir
    Path tempDir;

    @BeforeEach
    void useTempFolder() {
        FarmDataStore.getInstance().useFolder(tempDir);
    }

    @Test
    void aSavedFarmComesBackAfterARestart() {
        FarmJson.saveFarm(testFarm());

        FarmDataStore.getInstance().useFolder(tempDir); // forget the cache, as a restart would
        ItemContainer root = newRoot();
        FarmJson.loadFarm(root);

        assertEquals(177_500, root.getTotal(), DELTA);
        assertEquals(64_500, root.findByName("Barn").getTotal(), DELTA);
        FarmComponent cow = root.findByName("cow");
        assertFalse(cow.isContainer());
        assertEquals(545, cow.getLocationX(), DELTA);
        assertEquals(345, cow.getLocationY(), DELTA);
        assertEquals(50, cow.getLength(), DELTA);
        assertEquals(30, cow.getWidth(), DELTA);
        assertEquals(5, cow.getHeight(), DELTA);
        assertEquals("Livestock area", cow.getParent().getName());
    }

    @Test
    void aLoadedFarmStillMovesTogether() {
        FarmJson.saveFarm(testFarm());
        ItemContainer root = newRoot();
        FarmJson.loadFarm(root);

        root.findByName("Barn").move(450, 300);
        assertEquals(495, root.findByName("Cow").getLocationX(), DELTA);
        assertEquals(470, root.findByName("Milk storage").getLocationX(), DELTA);
    }

    @Test
    void containersAndItemsKeepTheirType() {
        NodeData data = FarmJson.toData(testFarm());
        assertEquals(NodeData.CONTAINER, data.getType());
        NodeData barn = data.getChildren().get(0);
        assertEquals("Barn", barn.getName());
        assertEquals(NodeData.CONTAINER, barn.getType());
        assertEquals(NodeData.ITEM, barn.getChildren().get(1).getType()); // Milk storage
    }

    @Test
    void loadingReplacesWhatWasThere() {
        FarmJson.saveFarm(testFarm());
        ItemContainer root = newRoot();
        root.add(new Item("Old shed", 100, 10, 10, 20, 20, 2));

        FarmJson.loadFarm(root);
        assertNull(root.findByName("Old shed"));
        assertEquals(4, root.getChild().size());
    }

    @Test
    void rootKeepsItsOwnValues() {
        NodeData data = FarmJson.toData(testFarm());
        data.setName("Something else");
        data.setLength(10);

        ItemContainer root = newRoot();
        FarmJson.copyInto(root, data);
        assertEquals("Root", root.getName());
        assertEquals(800, root.getLength(), DELTA);
        assertSame(root, root.findByName("Barn").getParent());
    }

    @Test
    void theDefaultFarmIsEmpty() {
        ItemContainer root = newRoot();
        FarmJson.loadFarm(root);
        assertTrue(root.getChild().isEmpty());
    }

    @Test
    void aBadTypeLeavesTheFarmAsItWas() {
        ItemContainer root = testFarm();
        NodeData data = FarmJson.toData(root);
        data.getChildren().get(0).setType("BARN");

        assertThrows(IllegalStateException.class, () -> FarmJson.copyInto(root, data));
        assertEquals(177_500, root.getTotal(), DELTA);
    }

    private static ItemContainer newRoot() {
        return new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
    }

    // The brief's test farm.
    private static ItemContainer testFarm() {
        ItemContainer root = newRoot();

        ItemContainer barn = new ItemContainer("Barn", 50_000, 500, 300, 270, 260, 30);
        ItemContainer livestock = new ItemContainer("Livestock area", 5_000, 520, 320, 140, 110, 10);
        livestock.add(new Item("Cow", 1_500, 545, 345, 50, 30, 5));
        barn.add(livestock);
        barn.add(new Item("Milk storage", 8_000, 520, 460, 120, 70, 12));

        ItemContainer storage = new ItemContainer("Storage building", 30_000, 30, 330, 230, 150, 25);
        storage.add(new Item("Tractor", 45_000, 50, 360, 80, 50, 10));
        storage.add(new Item("Tiller", 3_500, 150, 360, 70, 40, 4));

        ItemContainer command = new ItemContainer("Command center", 20_000, 30, 30, 160, 110, 15);
        command.add(new Item("Drone", 2_500, 85, 60, 40, 40, 1));

        root.add(barn);
        root.add(storage);
        root.add(command);
        root.add(new Item("Soy crop", 12_000, 230, 30, 540, 220, 3));
        return root;
    }
}
