package com.farmdashboard.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

/** Every farm command on the brief's test farm, in a temp folder so run-data/ is never touched. */
class FarmControllerTest {

    private static final double DELTA = 0.001;

    @TempDir
    Path tempDir;

    private ItemContainer root;
    private FarmController controller;

    @BeforeEach
    void loadTheTestFarm() {
        FarmDataStore.getInstance().useFolder(tempDir);
        root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        controller = new FarmController(root);
        controller.loadTestFarm();
    }

    @Test
    void loadTestFarmBuildsTheBriefsFarm() {
        assertEquals(4, root.getChild().size());
        assertEquals(177_500, root.getTotal(), DELTA);
    }

    @Test
    void addItemPutsTheItemInsideTheContainer() {
        ItemContainer barn = (ItemContainer) root.findByName("Barn");
        Item horse = controller.addItem(barn, new ComponentData("Horse", 3000, 600, 470, 40, 30, 5));
        assertSame(barn, horse.getParent());
        assertEquals(67_500, barn.getTotal(), DELTA);
    }

    @Test
    void addContainerPutsTheContainerInsideTheContainer() {
        ItemContainer shed = controller.addContainer(root, new ComponentData("Shed", 900, 300, 300, 80, 60, 10));
        assertSame(root, shed.getParent());
    }

    @Test
    void anItemCantHoldOtherItems() {
        FarmComponent cow = root.findByName("Cow");
        assertThrows(IllegalArgumentException.class,
                () -> controller.addItem(cow, new ComponentData("Calf", 500, 550, 350, 20, 10, 3)));
    }

    @Test
    void deleteRemovesTheNodeAndEverythingInsideIt() {
        assertEquals(3, controller.countContents(root.findByName("Barn")));
        controller.delete(root.findByName("Barn"));
        assertNull(root.findByName("Barn"));
        assertNull(root.findByName("Cow"));
        assertEquals(113_000, root.getTotal(), DELTA);
    }

    @Test
    void rootCantBeDeletedMovedOrResized() {
        assertThrows(IllegalArgumentException.class, () -> controller.delete(root));
        assertThrows(IllegalArgumentException.class, () -> controller.changeLocationX(root, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.changeWidth(root, 500));
    }

    @Test
    void renameAndChangePrice() {
        controller.rename(root.findByName("Cow"), "Bessie");
        controller.changePrice(root.findByName("Bessie"), 2000);
        assertEquals(2000, root.findByName("Bessie").getPrice(), DELTA);
        assertThrows(IllegalArgumentException.class, () -> controller.rename(root.findByName("Bessie"), " "));
    }

    @Test
    void movingTheBarnMovesEverythingInsideIt() {
        FarmComponent barn = root.findByName("Barn");
        controller.changeLocationX(barn, 450);
        controller.changeLocationY(barn, 300);
        assertEquals(470, root.findByName("Livestock area").getLocationX(), DELTA);
        assertEquals(495, root.findByName("Cow").getLocationX(), DELTA);
        assertEquals(345, root.findByName("Cow").getLocationY(), DELTA);
        assertEquals(470, root.findByName("Milk storage").getLocationX(), DELTA);
    }

    @Test
    void changeDimensions() {
        FarmComponent tiller = root.findByName("Tiller");
        controller.changeLength(tiller, 75);
        controller.changeWidth(tiller, 45);
        controller.changeHeight(tiller, 0);
        assertEquals(75, tiller.getLength(), DELTA);
        assertEquals(45, tiller.getWidth(), DELTA);
        assertEquals(0, tiller.getHeight(), DELTA);
    }

    @Test
    void badNumbersAreRefused() {
        FarmComponent cow = root.findByName("Cow");
        assertThrows(IllegalArgumentException.class, () -> controller.changeWidth(cow, 0));
        assertThrows(IllegalArgumentException.class, () -> controller.changePrice(cow, -1));
        assertThrows(IllegalArgumentException.class, () -> controller.changeLocationY(cow, -5));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addItem(root, new ComponentData("", 1, 1, 1, 1, 1, 1)));
    }

    @Test
    void everyCommandSavesTheWholeFarm() {
        controller.changeLocationX(root.findByName("Barn"), 450);

        FarmDataStore.getInstance().useFolder(tempDir); // forget the cache, as a restart would
        ItemContainer reopened = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
        FarmJson.copyInto(reopened, FarmDataStore.getInstance().get(DataFile.FARM, NodeData.class));
        assertEquals(450, reopened.findByName("Barn").getLocationX(), DELTA);
        assertEquals(495, reopened.findByName("Cow").getLocationX(), DELTA);
        assertTrue(reopened.findByName("Milk storage").isInside(reopened.findByName("Barn")));
    }
}
