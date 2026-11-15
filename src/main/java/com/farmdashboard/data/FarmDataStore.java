package com.farmdashboard.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Singleton: the only class that reads or writes the JSON files.
 * The defaults ship inside the app. The app can't
 * write inside its own jar, so on first use each file is copied to run-data/ and used from there.
 * Every caller gets the same object for a file, so a change made in one place is seen everywhere.
 */
public final class FarmDataStore {

    private static FarmDataStore instance;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<DataFile, Object> cache = new EnumMap<>(DataFile.class);
    private final Map<DataFile, List<Runnable>> listeners = new EnumMap<>(DataFile.class);
    private Path folder = Path.of("run-data");

    private FarmDataStore() {
    }

    public static synchronized FarmDataStore getInstance() {
        if (instance == null) {
            instance = new FarmDataStore();
        }
        return instance;
    }

    public <T> T get(DataFile file, Class<T> type) {
        if (!type.isAssignableFrom(file.dataClass())) {
            throw new IllegalArgumentException(file.fileName() + " holds "
                    + file.dataClass().getSimpleName() + ", not " + type.getSimpleName() + ".");
        }
        Object data = cache.get(file);
        if (data == null) {
            data = read(file);
            cache.put(file, data);
        }
        return type.cast(data);
    }

    public void put(DataFile file, Object data) {
        if (!file.dataClass().isInstance(data)) {
            throw new IllegalArgumentException(file.fileName() + " holds "
                    + file.dataClass().getSimpleName() + ", not "
                    + (data == null ? "null" : data.getClass().getSimpleName()) + ".");
        }
        cache.put(file, data);
    }

    public void save(DataFile file) {
        Object data = cache.get(file);
        if (data == null) {
            throw new IllegalStateException("Nothing to save for " + file.fileName()
                    + ": call get() or put() first.");
        }
        Path path = folder.resolve(file.fileName());
        try {
            Files.createDirectories(folder);
            Files.writeString(path, gson.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't save " + path, e);
        }
        runListeners(file);
    }

    public void onChange(DataFile file, Runnable listener) {
        listeners.computeIfAbsent(file, f -> new ArrayList<>()).add(listener);
    }

    public void resetAll() {
        for (DataFile file : DataFile.values()) {
            copyDefault(file, true);
        }
        cache.clear();
        for (DataFile file : DataFile.values()) {
            runListeners(file);
        }
    }

    public void useFolder(Path folder) {
        this.folder = folder;
        cache.clear();
        listeners.clear();
    }

    private Object read(DataFile file) {
        Path path = folder.resolve(file.fileName());
        if (Files.notExists(path) && !copyDefault(file, false)) {
            throw new IllegalStateException("No " + file.fileName()
                    + " in run-data/ or in src/main/resources/com/farmdashboard/data/.");
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object data = gson.fromJson(reader, file.dataClass());
            if (data == null) {
                throw new IllegalStateException(path + " is empty.");
            }
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read " + path, e);
        } catch (JsonParseException e) {
            throw new IllegalStateException(path + " isn't valid JSON: " + e.getMessage(), e);
        }
    }

    private boolean copyDefault(DataFile file, boolean replace) {
        try (InputStream in = FarmDataStore.class.getResourceAsStream(file.fileName())) {
            if (in == null) {
                return false;
            }
            Files.createDirectories(folder);
            Path target = folder.resolve(file.fileName());
            if (replace) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(in, target);
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't copy the default " + file.fileName(), e);
        }
    }

    private void runListeners(DataFile file) {
        for (Runnable listener : listeners.getOrDefault(file, List.of())) {
            listener.run();
        }
    }
}
