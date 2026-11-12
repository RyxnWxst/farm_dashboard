package com.farmdashboard.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the composite model (FarmComponent / ItemContainer / Item) against a small test farm:
 *
 * Root
 * ├── Barn (container)
 * │   ├── Livestock Area (container)
 * │   │   └── Cow
 * │   └── Milk Storage
 * ├── Storage Building (container)
 * │   ├── Tractor
 * │   └── Tiller
 * ├── Command Center (container)
 * │   └── Drone
 * └── Soy Crop
 */
class FarmCompositeTest {

    private static final double DELTA = 1e-9;

    private ItemContainer root;
    private ItemContainer barn;
    private ItemContainer livestockArea;
    private Item cow;
    private Item milkStorage;
    private ItemContainer storageBuilding;
    private Item tractor;
    private Item tiller;
    private ItemContainer commandCenter;
    private Item drone;
    private Item soyCrop;

    @BeforeEach
    void buildTestFarm() {
        //                         name                price     x    y   length width height
        root            = new ItemContainer("Root",             0,     0,   0, 1000, 1000,  0);
        barn            = new ItemContainer("Barn",         50000,    50,  50,  300,  200, 30);
        livestockArea   = new ItemContainer("Livestock Area", 5000,   60,  60,  150,  100,  5);
        cow             = new Item("Cow",                    1500,    70,  70,   10,    5,  5);
        milkStorage     = new Item("Milk Storage",           3000,   250,  60,   40,   40, 10);
        storageBuilding = new ItemContainer("Storage Building", 40000, 400, 50, 200,  150, 25);
        tractor         = new Item("Tractor",               75000,   410,  60,   30,   20, 10);
        tiller          = new Item("Tiller",                 8000,   450,  60,   20,   15,  5);
        commandCenter   = new ItemContainer("Command Center", 25000, 650, 50,  100,  100, 15);
        drone           = new Item("Drone",                  2000,   660,  60,    5,    5,  1);
        soyCrop         = new Item("Soy Crop",              12000,    50, 400,  500,  300,  2);

        root.add(barn);
        barn.add(livestockArea);
        livestockArea.add(cow);
        barn.add(milkStorage);
        root.add(storageBuilding);
        storageBuilding.add(tractor);
        storageBuilding.add(tiller);
        root.add(commandCenter);
        commandCenter.add(drone);
        root.add(soyCrop);
    }

    // ---------- structure ----------

    @Test
    @DisplayName("Root holds barn, storage building, command center and soy crop in order")
    void rootChildren() {
        assertEquals(List.of(barn, storageBuilding, commandCenter, soyCrop), root.getChild());
    }

    @Test
    @DisplayName("Each container holds exactly its expected children")
    void containerChildren() {
        assertEquals(List.of(livestockArea, milkStorage), barn.getChild());
        assertEquals(List.of(cow), livestockArea.getChild());
        assertEquals(List.of(tractor, tiller), storageBuilding.getChild());
        assertEquals(List.of(drone), commandCenter.getChild());
    }

    @Test
    @DisplayName("Every node's parent points to the container it was added to")
    void parentLinks() {
        assertNull(root.getParent());
        assertSame(root, barn.getParent());
        assertSame(barn, livestockArea.getParent());
        assertSame(livestockArea, cow.getParent());
        assertSame(barn, milkStorage.getParent());
        assertSame(root, storageBuilding.getParent());
        assertSame(storageBuilding, tractor.getParent());
        assertSame(storageBuilding, tiller.getParent());
        assertSame(root, commandCenter.getParent());
        assertSame(commandCenter, drone.getParent());
        assertSame(root, soyCrop.getParent());
    }

    @Test
    @DisplayName("Containers report isContainer() true, items false")
    void isContainer() {
        for (FarmComponent c : List.of(root, barn, livestockArea, storageBuilding, commandCenter)) {
            assertTrue(c.isContainer(), c.getName());
        }
        for (FarmComponent i : List.of(cow, milkStorage, tractor, tiller, drone, soyCrop)) {
            assertFalse(i.isContainer(), i.getName());
        }
    }

    @Test
    @DisplayName("Items (leaves) have no children")
    void leavesHaveNoChildren() {
        for (FarmComponent i : List.of(cow, milkStorage, tractor, tiller, drone, soyCrop)) {
            assertTrue(i.getChild().isEmpty(), i.getName());
        }
    }

    @Test
    @DisplayName("getChild() is read-only")
    void childListIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> root.getChild().add(new Item("X", 0, 0, 0, 1, 1, 1)));
        assertThrows(UnsupportedOperationException.class, () -> barn.getChild().clear());
        assertThrows(UnsupportedOperationException.class, () -> cow.getChild().add(tractor));
    }

    // ---------- getTotal ----------

    @Test
    @DisplayName("getTotal() on a leaf is just its own price")
    void leafTotal() {
        assertEquals(1500, cow.getTotal(), DELTA);
        assertEquals(12000, soyCrop.getTotal(), DELTA);
    }

    @Test
    @DisplayName("getTotal() on containers sums their own price and all descendants")
    void containerTotals() {
        assertEquals(5000 + 1500, livestockArea.getTotal(), DELTA);
        assertEquals(50000 + 5000 + 1500 + 3000, barn.getTotal(), DELTA);
        assertEquals(40000 + 75000 + 8000, storageBuilding.getTotal(), DELTA);
        assertEquals(25000 + 2000, commandCenter.getTotal(), DELTA);
        assertEquals(221500, root.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Changing a deep item's price is reflected in every ancestor's total")
    void priceChangePropagates() {
        cow.setPrice(2500);
        assertEquals(7500, livestockArea.getTotal(), DELTA);
        assertEquals(60500, barn.getTotal(), DELTA);
        assertEquals(222500, root.getTotal(), DELTA);
    }

    // ---------- findByName ----------

    @Test
    @DisplayName("findByName() locates nodes at every depth")
    void findByNameAtAllDepths() {
        assertSame(root, root.findByName("Root"));
        assertSame(barn, root.findByName("Barn"));
        assertSame(livestockArea, root.findByName("Livestock Area"));
        assertSame(cow, root.findByName("Cow"));
        assertSame(tiller, root.findByName("Tiller"));
        assertSame(drone, root.findByName("Drone"));
        assertSame(soyCrop, root.findByName("Soy Crop"));
    }

    @Test
    @DisplayName("findByName() is case-insensitive")
    void findByNameIgnoresCase() {
        assertSame(milkStorage, root.findByName("milk storage"));
        assertSame(commandCenter, root.findByName("COMMAND CENTER"));
    }

    @Test
    @DisplayName("findByName() returns null when absent or outside the searched subtree")
    void findByNameMissing() {
        assertNull(root.findByName("Chicken"));
        assertNull(barn.findByName("Tractor"));
        assertNull(cow.findByName("Barn"));
    }

    // ---------- add / remove / clear ----------

    @Test
    @DisplayName("Adding a node to a new container moves it out of its old one")
    void reparenting() {
        commandCenter.add(tractor);

        assertSame(commandCenter, tractor.getParent());
        assertEquals(List.of(tiller), storageBuilding.getChild());
        assertEquals(List.of(drone, tractor), commandCenter.getChild());
        assertEquals(221500, root.getTotal(), DELTA);
        assertEquals(40000 + 8000, storageBuilding.getTotal(), DELTA);
        assertEquals(25000 + 2000 + 75000, commandCenter.getTotal(), DELTA);
    }

    @Test
    @DisplayName("A container cannot be added to itself or to one of its descendants")
    void cyclesRejected() {
        assertThrows(IllegalArgumentException.class, () -> barn.add(barn));
        assertThrows(IllegalArgumentException.class, () -> livestockArea.add(barn));
        assertThrows(IllegalArgumentException.class, () -> livestockArea.add(root));

        // tree is unchanged after the failed adds
        assertSame(root, barn.getParent());
        assertEquals(List.of(livestockArea, milkStorage), barn.getChild());
    }

    @Test
    @DisplayName("remove() detaches a child, clears its parent and drops it from totals")
    void removeChild() {
        root.remove(soyCrop);

        assertNull(soyCrop.getParent());
        assertFalse(root.getChild().contains(soyCrop));
        assertNull(root.findByName("Soy Crop"));
        assertEquals(221500 - 12000, root.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Removing a whole subtree keeps the subtree intact")
    void removeSubtree() {
        root.remove(barn);

        assertNull(barn.getParent());
        assertNull(root.findByName("Cow"));
        assertSame(cow, barn.findByName("Cow"));
        assertSame(livestockArea, cow.getParent());
        assertEquals(59500, barn.getTotal(), DELTA);
    }

    @Test
    @DisplayName("remove() of a non-child does nothing")
    void removeNonChild() {
        root.remove(cow);
        assertSame(livestockArea, cow.getParent());
        assertEquals(4, root.getChild().size());
    }

    @Test
    @DisplayName("clear() empties a container and orphans its former children")
    void clearContainer() {
        root.clear();

        assertTrue(root.getChild().isEmpty());
        assertEquals(0, root.getTotal(), DELTA);
        for (FarmComponent c : List.of(barn, storageBuilding, commandCenter, soyCrop)) {
            assertNull(c.getParent(), c.getName());
        }
        // grandchildren stay attached to their own containers
        assertSame(storageBuilding, tractor.getParent());
    }

    // ---------- geometry ----------

    @Test
    @DisplayName("Every node in the test farm lies inside its parent's rectangle")
    void childrenInsideParents() {
        for (FarmComponent c : List.of(barn, livestockArea, cow, milkStorage, storageBuilding,
                tractor, tiller, commandCenter, drone, soyCrop)) {
            assertTrue(c.isInside(c.getParent()), c.getName() + " should be inside " + c.getParent());
        }
        assertFalse(tractor.isInside(barn));
        assertFalse(barn.isInside(cow));
    }

    @Test
    @DisplayName("Centre point is the middle of the footprint")
    void centre() {
        assertEquals(200, barn.getCenterX(), DELTA);
        assertEquals(150, barn.getCenterY(), DELTA);
        assertEquals(75, cow.getCenterX(), DELTA);
        assertEquals(72.5, cow.getCenterY(), DELTA);
    }

    @Test
    @DisplayName("Moving a container moves its whole subtree by the same offset")
    void moveCascades() {
        barn.move(100, 500);   // offset (+50, +450)

        assertEquals(100, barn.getLocationX(), DELTA);
        assertEquals(500, barn.getLocationY(), DELTA);
        assertEquals(110, livestockArea.getLocationX(), DELTA);
        assertEquals(510, livestockArea.getLocationY(), DELTA);
        assertEquals(120, cow.getLocationX(), DELTA);
        assertEquals(520, cow.getLocationY(), DELTA);
        assertEquals(300, milkStorage.getLocationX(), DELTA);
        assertEquals(510, milkStorage.getLocationY(), DELTA);

        // children still inside their containers after the move
        assertTrue(cow.isInside(livestockArea));
        assertTrue(livestockArea.isInside(barn));
        assertTrue(milkStorage.isInside(barn));

        // siblings are untouched
        assertEquals(400, storageBuilding.getLocationX(), DELTA);
        assertEquals(410, tractor.getLocationX(), DELTA);
    }

    @Test
    @DisplayName("Moving a leaf moves only that leaf")
    void moveLeaf() {
        drone.move(700, 120);

        assertEquals(700, drone.getLocationX(), DELTA);
        assertEquals(120, drone.getLocationY(), DELTA);
        assertEquals(650, commandCenter.getLocationX(), DELTA);
        assertEquals(50, commandCenter.getLocationY(), DELTA);
        assertTrue(drone.isInside(commandCenter));
    }
}
