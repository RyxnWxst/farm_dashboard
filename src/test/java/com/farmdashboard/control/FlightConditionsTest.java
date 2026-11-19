package com.farmdashboard.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.farmdashboard.control.FlightConditions.RatedForecast;
import com.farmdashboard.control.FlightConditions.Rating;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.WeatherData;
import com.farmdashboard.data.WeatherData.ForecastEntry;

/** Checks the flight ratings against the default weather.json, and the flight log in drone.json. */
class FlightConditionsTest {

    private static final double DELTA = 0.001;
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final Clock TWO_THIRTY_PM = Clock.fixed(
            LocalDateTime.of(2026, 11, 15, 14, 30, 0).atZone(ZONE).toInstant(), ZONE);

    @TempDir
    Path tempDir;

    private FarmDataStore store;
    private FlightConditions conditions;

    @BeforeEach
    void useTempFolder() {
        store = FarmDataStore.getInstance();
        store.useFolder(tempDir); // the Singleton lives across tests, so start each one fresh
        conditions = new FlightConditions(TWO_THIRTY_PM);
    }

    @Test
    void calmDryWeatherIsSafe() {
        assertEquals(Rating.SAFE, FlightConditions.rate(9.9, 29.9));
        assertEquals(Rating.SAFE, FlightConditions.rate(0, 0));
    }

    @Test
    void theSafeLimitsThemselvesAreOnlyCaution() {
        assertEquals(Rating.CAUTION, FlightConditions.rate(10, 0));
        assertEquals(Rating.CAUTION, FlightConditions.rate(0, 30));
    }

    @Test
    void twentyMphAndJustUnderFiftyPercentAreStillCaution() {
        assertEquals(Rating.CAUTION, FlightConditions.rate(20, 49.9));
    }

    @Test
    void windOverTwentyOrRainFromFiftyIsUnsafe() {
        assertEquals(Rating.UNSAFE, FlightConditions.rate(20.1, 0));
        assertEquals(Rating.UNSAFE, FlightConditions.rate(0, 50));
        assertEquals(Rating.UNSAFE, FlightConditions.rate(5, 100));
    }

    @Test
    void impossibleWeatherIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> FlightConditions.rate(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> FlightConditions.rate(0, -1));
        assertThrows(IllegalArgumentException.class, () -> FlightConditions.rate(0, 101));
    }

    @Test
    void theDefaultWeatherIsCautionNow() {
        assertEquals(Rating.CAUTION, conditions.now()); // 12 mph, 10% rain
    }

    @Test
    void theDefaultForecastIsRatedInOrder() {
        List<RatedForecast> forecast = conditions.forecast();

        assertEquals(5, forecast.size());
        assertSlot(forecast.get(0), "3 PM", Rating.CAUTION);
        assertSlot(forecast.get(1), "5 PM", Rating.CAUTION);
        assertSlot(forecast.get(2), "6 PM", Rating.UNSAFE);
        assertSlot(forecast.get(3), "7 PM", Rating.UNSAFE);
        assertSlot(forecast.get(4), "6 AM tomorrow", Rating.SAFE);
    }

    @Test
    void theFirstUnsafeSlotInTheDefaultForecastIs6Pm() {
        ForecastEntry unsafe = conditions.firstUnsafe().orElseThrow();
        assertEquals("6 PM", unsafe.getLabel());
        assertEquals("18:00", unsafe.getTime());
    }

    @Test
    void aForecastWithNoUnsafeSlotHasNoFirstUnsafe() {
        WeatherData calm = new WeatherData();
        calm.getForecast().add(slot("3 PM", 5, 0));
        calm.getForecast().add(slot("5 PM", 15, 40));
        store.put(DataFile.WEATHER, calm);

        assertTrue(conditions.firstUnsafe().isEmpty());
    }

    @Test
    void aMissingForecastCountsAsEmpty() {
        WeatherData noForecast = new WeatherData();
        noForecast.setForecast(null);
        store.put(DataFile.WEATHER, noForecast);

        assertTrue(conditions.forecast().isEmpty());
        assertTrue(conditions.firstUnsafe().isEmpty());
    }

    @Test
    void theDefaultLogHasNoLastFlight() {
        assertTrue(conditions.lastFlight().isEmpty());
    }

    @Test
    void aLoggedVisitIsSavedWithTheCurrentTime() {
        conditions.logFlight(FlightLogEntry.VISIT, "Cow", 5.34);

        store.useFolder(tempDir); // forget the cached copy, as a restart would
        List<FlightLogEntry> log = store.get(DataFile.DRONE, DroneData.class).getFlightLog();
        assertEquals(1, log.size());
        assertEquals(FlightLogEntry.VISIT, log.get(0).getType());
        assertEquals("Cow", log.get(0).getTarget());
        assertEquals("2026-11-15T14:30:00", log.get(0).getStartTime());
        assertEquals(5.34, log.get(0).getDurationSeconds(), DELTA);
    }

    @Test
    void lastFlightIsTheNewestRow() {
        conditions.logFlight(FlightLogEntry.VISIT, "Cow", 5.34);
        conditions.logFlight(FlightLogEntry.SCAN, null, 20.94);

        FlightLogEntry last = conditions.lastFlight().orElseThrow();
        assertEquals(FlightLogEntry.SCAN, last.getType());
        assertNull(last.getTarget());
        assertEquals(20.94, last.getDurationSeconds(), DELTA);
    }

    @Test
    void loggingWorksWhenTheLogIsNull() {
        DroneData drone = store.get(DataFile.DRONE, DroneData.class);
        drone.setFlightLog(null);

        conditions.logFlight(FlightLogEntry.SCAN, null, 20.94);
        assertEquals(1, drone.getFlightLog().size());
    }

    @Test
    void badFlightsAreRefusedAndNothingIsLogged() {
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight("HOVER", "Cow", 1));
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight(null, "Cow", 1));
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight(FlightLogEntry.VISIT, null, 1));
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight(FlightLogEntry.VISIT, " ", 1));
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight(FlightLogEntry.SCAN, "Cow", 1));
        assertThrows(IllegalArgumentException.class, () -> conditions.logFlight(FlightLogEntry.SCAN, null, -1));

        assertTrue(conditions.lastFlight().isEmpty());
    }

    private static void assertSlot(RatedForecast slot, String label, Rating rating) {
        assertEquals(label, slot.entry().getLabel());
        assertEquals(rating, slot.rating());
    }

    private static ForecastEntry slot(String label, double windMph, double rainPercent) {
        ForecastEntry entry = new ForecastEntry();
        entry.setLabel(label);
        entry.setWindMph(windMph);
        entry.setRainPercent(rainPercent);
        return entry;
    }
}
