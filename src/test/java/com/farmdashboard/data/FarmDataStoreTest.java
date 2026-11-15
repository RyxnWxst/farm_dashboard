package com.farmdashboard.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks FarmDataStore in a temp folder, so run-data/ is never touched. */
class FarmDataStoreTest {

    private static final double DELTA = 0.001;

    @TempDir
    Path tempDir;

    private FarmDataStore store;

    @BeforeEach
    void useTempFolder() {
        store = FarmDataStore.getInstance();
        store.useFolder(tempDir); // the Singleton lives across tests, so start each one fresh
    }

    @Test
    void thereIsOnlyOneStore() {
        assertSame(FarmDataStore.getInstance(), FarmDataStore.getInstance());
    }

    @Test
    void everyCallerGetsTheSameObject() {
        assertSame(store.get(DataFile.FARM, NodeData.class), store.get(DataFile.FARM, NodeData.class));
    }

    @Test
    void theFirstGetCopiesTheDefault() {
        NodeData farm = store.get(DataFile.FARM, NodeData.class);
        assertTrue(Files.exists(tempDir.resolve("farm.json")));
        assertEquals("Root", farm.getName());
        assertEquals(NodeData.CONTAINER, farm.getType());
        assertEquals(800, farm.getLength(), DELTA);
        assertEquals(600, farm.getWidth(), DELTA);
        assertTrue(farm.getChildren().isEmpty());
    }

    @Test
    void savedChangesSurviveAReload() {
        NodeData farm = store.get(DataFile.FARM, NodeData.class);
        farm.getChildren().add(node(NodeData.ITEM, "Cow", 1500));
        store.save(DataFile.FARM);

        store.useFolder(tempDir); // forget the cached copy, as a restart would
        NodeData reloaded = store.get(DataFile.FARM, NodeData.class);
        assertEquals(1, reloaded.getChildren().size());
        assertEquals("Cow", reloaded.getChildren().get(0).getName());
        assertEquals(1500, reloaded.getChildren().get(0).getPrice(), DELTA);
    }

    @Test
    void putReplacesTheData() {
        store.put(DataFile.FARM, node(NodeData.CONTAINER, "New farm", 0));
        store.save(DataFile.FARM);

        store.useFolder(tempDir);
        assertEquals("New farm", store.get(DataFile.FARM, NodeData.class).getName());
    }

    @Test
    void theWrongClassIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> store.put(DataFile.FARM, "not a farm"));
        assertThrows(IllegalArgumentException.class, () -> store.get(DataFile.FARM, String.class));
    }

    @Test
    void aListenerRunsOncePerSave() {
        int[] runs = {0};
        store.onChange(DataFile.FARM, () -> runs[0]++);
        store.get(DataFile.FARM, NodeData.class);
        store.save(DataFile.FARM);
        store.save(DataFile.FARM);
        assertEquals(2, runs[0]);
    }

    @Test
    void resetAllBringsTheDefaultBack() {
        store.get(DataFile.FARM, NodeData.class).getChildren().add(node(NodeData.ITEM, "Cow", 1500));
        store.save(DataFile.FARM);

        store.resetAll();
        assertTrue(store.get(DataFile.FARM, NodeData.class).getChildren().isEmpty());
    }

    @Test
    void aBrokenFileGivesAClearError() throws IOException {
        Files.writeString(tempDir.resolve("farm.json"), "{ this is not JSON");
        assertThrows(IllegalStateException.class, () -> store.get(DataFile.FARM, NodeData.class));
    }

    private static NodeData node(String type, String name, double price) {
        NodeData node = new NodeData();
        node.setType(type);
        node.setName(name);
        node.setPrice(price);
        return node;
    }
}