package com.farmdashboard.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the default drone.json and weather.json, and that both survive a save and reload. */
class DroneAndWeatherDataTest {

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
    void droneSettingsAndFlightLogSurviveAReload() {
        DroneData drone = new DroneData();
        drone.setSpeed(250);
        drone.setHoverSeconds(1);
        drone.setLaneSpacing(100);
        drone.setMargin(50);
        drone.getFlightLog().add(flight(DroneData.FlightLogEntry.VISIT, "Cow", 5.3));
        drone.getFlightLog().add(flight(DroneData.FlightLogEntry.SCAN, null, 21));
        store.put(DataFile.DRONE, drone);
        store.save(DataFile.DRONE);

        store.useFolder(tempDir); // forget the cached copy, as a restart would
        DroneData reloaded = store.get(DataFile.DRONE, DroneData.class);
        assertEquals(250, reloaded.getSpeed(), DELTA);
        assertEquals(1, reloaded.getHoverSeconds(), DELTA);
        assertEquals(100, reloaded.getLaneSpacing(), DELTA);
        assertEquals(50, reloaded.getMargin(), DELTA);
        assertEquals(2, reloaded.getFlightLog().size());

        DroneData.FlightLogEntry visit = reloaded.getFlightLog().get(0);
        assertEquals(DroneData.FlightLogEntry.VISIT, visit.getType());
        assertEquals("Cow", visit.getTarget());
        assertEquals("2026-11-15T14:30:00", visit.getStartTime());
        assertEquals(5.3, visit.getDurationSeconds(), DELTA);

        DroneData.FlightLogEntry scan = reloaded.getFlightLog().get(1);
        assertEquals(DroneData.FlightLogEntry.SCAN, scan.getType());
        assertNull(scan.getTarget());
    }

    @Test
    void currentWeatherAndForecastSurviveAReload() {
        WeatherData weather = new WeatherData();
        weather.setWindMph(12);
        weather.setWindDirection("NW");
        weather.setRainPercent(20);
        weather.setTemperatureF(64);
        WeatherData.ForecastEntry evening = new WeatherData.ForecastEntry();
        evening.setLabel("6 PM");
        evening.setTime("18:00");
        evening.setWindMph(24);
        evening.setRainPercent(55);
        weather.getForecast().add(evening);
        store.put(DataFile.WEATHER, weather);
        store.save(DataFile.WEATHER);

        store.useFolder(tempDir);
        WeatherData reloaded = store.get(DataFile.WEATHER, WeatherData.class);
        assertEquals(12, reloaded.getWindMph(), DELTA);
        assertEquals("NW", reloaded.getWindDirection());
        assertEquals(20, reloaded.getRainPercent(), DELTA);
        assertEquals(64, reloaded.getTemperatureF(), DELTA);
        assertEquals(1, reloaded.getForecast().size());

        WeatherData.ForecastEntry entry = reloaded.getForecast().get(0);
        assertEquals("6 PM", entry.getLabel());
        assertEquals("18:00", entry.getTime());
        assertEquals(24, entry.getWindMph(), DELTA);
        assertEquals(55, entry.getRainPercent(), DELTA);
    }

    @Test
    void theDefaultDroneFileLoads() {
        DroneData drone = store.get(DataFile.DRONE, DroneData.class);
        assertEquals(250, drone.getSpeed(), DELTA);
        assertEquals(1, drone.getHoverSeconds(), DELTA);
        assertEquals(100, drone.getLaneSpacing(), DELTA);
        assertEquals(50, drone.getMargin(), DELTA);
        assertTrue(drone.getFlightLog().isEmpty());
    }

    @Test
    void theDefaultWeatherFileLoads() {
        WeatherData weather = store.get(DataFile.WEATHER, WeatherData.class);
        assertEquals(12, weather.getWindMph(), DELTA);
        assertEquals("NW", weather.getWindDirection());
        assertEquals(10, weather.getRainPercent(), DELTA);
        assertEquals(72, weather.getTemperatureF(), DELTA);

        List<WeatherData.ForecastEntry> forecast = weather.getForecast();
        assertEquals(5, forecast.size());
        assertForecast(forecast.get(0), "3 PM", "15:00", 14, 10);
        assertForecast(forecast.get(1), "5 PM", "17:00", 19, 35);
        assertForecast(forecast.get(2), "6 PM", "18:00", 24, 50);
        assertForecast(forecast.get(3), "7 PM", "19:00", 22, 60);
        assertForecast(forecast.get(4), "6 AM tomorrow", "06:00", 5, 0);
    }

    private static void assertForecast(WeatherData.ForecastEntry entry, String label, String time,
            double windMph, double rainPercent) {
        assertEquals(label, entry.getLabel());
        assertEquals(time, entry.getTime());
        assertEquals(windMph, entry.getWindMph(), DELTA);
        assertEquals(rainPercent, entry.getRainPercent(), DELTA);
    }

    private static DroneData.FlightLogEntry flight(String type, String target, double seconds) {
        DroneData.FlightLogEntry entry = new DroneData.FlightLogEntry();
        entry.setType(type);
        entry.setTarget(target);
        entry.setStartTime("2026-11-15T14:30:00");
        entry.setDurationSeconds(seconds);
        return entry;
    }
}
