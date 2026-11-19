package com.farmdashboard.control;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.WeatherData;
import com.farmdashboard.data.WeatherData.ForecastEntry;

/** Rates the weather for flying, now and for each forecast slot, and keeps the drone's flight log. */
public class FlightConditions {

    /** Green, yellow and red: fly, fly with care, don't fly. */
    public enum Rating {
        SAFE, CAUTION, UNSAFE
    }

    /** One forecast slot together with how safe it is to fly then. */
    public record RatedForecast(ForecastEntry entry, Rating rating) {
    }

    private static final double SAFE_WIND_BELOW = 10;
    private static final double SAFE_RAIN_BELOW = 30;
    private static final double UNSAFE_WIND_ABOVE = 20;
    private static final double UNSAFE_RAIN_FROM = 50;

    private final Clock clock;

    public FlightConditions() {
        this(Clock.systemDefaultZone());
    }

    // Tests pass a fixed clock so the logged start time is known.
    FlightConditions(Clock clock) {
        this.clock = clock;
    }

    /** SAFE below 10 mph and 30% rain, UNSAFE above 20 mph or at 50% rain or more, else CAUTION. */
    public static Rating rate(double windMph, double rainPercent) {
        if (windMph < 0) {
            throw new IllegalArgumentException("Wind speed can't be negative.");
        }
        if (rainPercent < 0 || rainPercent > 100) {
            throw new IllegalArgumentException("Chance of rain must be between 0 and 100%.");
        }
        if (windMph > UNSAFE_WIND_ABOVE || rainPercent >= UNSAFE_RAIN_FROM) {
            return Rating.UNSAFE;
        }
        if (windMph < SAFE_WIND_BELOW && rainPercent < SAFE_RAIN_BELOW) {
            return Rating.SAFE;
        }
        return Rating.CAUTION;
    }

    /** The rating for the current conditions in weather.json. */
    public Rating now() {
        WeatherData weather = weather();
        return rate(weather.getWindMph(), weather.getRainPercent());
    }

    /** Every forecast slot in weather.json, in file order, with its rating. */
    public List<RatedForecast> forecast() {
        List<RatedForecast> rated = new ArrayList<>();
        for (ForecastEntry entry : forecastEntries()) {
            rated.add(new RatedForecast(entry, rate(entry.getWindMph(), entry.getRainPercent())));
        }
        return List.copyOf(rated);
    }

    /** The earliest forecast slot that is UNSAFE, for the "Unsafe flying at 6 PM" alert. */
    public Optional<ForecastEntry> firstUnsafe() {
        for (RatedForecast slot : forecast()) {
            if (slot.rating() == Rating.UNSAFE) {
                return Optional.of(slot.entry());
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a flight to drone.json's log, stamped with the current time, and saves the file.
     * A VISIT needs the target's name; a SCAN has no target.
     */
    public void logFlight(String type, String targetName, double seconds) {
        if (FlightLogEntry.VISIT.equals(type)) {
            if (targetName == null || targetName.isBlank()) {
                throw new IllegalArgumentException("A visit needs the name of the item visited.");
            }
        } else if (FlightLogEntry.SCAN.equals(type)) {
            if (targetName != null) {
                throw new IllegalArgumentException("A scan covers the whole farm, so it has no target.");
            }
        } else {
            throw new IllegalArgumentException("A flight's type must be " + FlightLogEntry.VISIT
                    + " or " + FlightLogEntry.SCAN + ", not \"" + type + "\".");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("A flight can't take a negative time.");
        }

        FlightLogEntry entry = new FlightLogEntry();
        entry.setType(type);
        entry.setTarget(targetName);
        entry.setStartTime(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        entry.setDurationSeconds(seconds);

        FarmDataStore store = FarmDataStore.getInstance();
        DroneData drone = store.get(DataFile.DRONE, DroneData.class);
        if (drone.getFlightLog() == null) {
            drone.setFlightLog(new ArrayList<>()); // the JSON may set the log to null
        }
        drone.getFlightLog().add(entry);
        store.save(DataFile.DRONE);
    }

    /** The most recently logged flight, or empty if none has been flown. */
    public Optional<FlightLogEntry> lastFlight() {
        List<FlightLogEntry> log = FarmDataStore.getInstance()
                .get(DataFile.DRONE, DroneData.class).getFlightLog();
        if (log == null || log.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(log.get(log.size() - 1));
    }

    // Read on every call rather than kept, so a save or resetAll() is always seen.
    private static WeatherData weather() {
        return FarmDataStore.getInstance().get(DataFile.WEATHER, WeatherData.class);
    }

    private static List<ForecastEntry> forecastEntries() {
        List<ForecastEntry> forecast = weather().getForecast();
        return forecast == null ? List.of() : forecast;
    }
}
