package com.farmdashboard.data;

import java.util.ArrayList;
import java.util.List;

/** How weather.json is stored: the current conditions and the forecast. */
public class WeatherData {

    private double windMph;
    private String windDirection; // compass point, e.g. "NW"
    private double rainPercent;   // chance of rain, 0-100
    private double temperatureF;
    private List<ForecastEntry> forecast = new ArrayList<>();

    public WeatherData() {
    }

    public double getWindMph() {
        return windMph;
    }

    public void setWindMph(double windMph) {
        this.windMph = windMph;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public double getRainPercent() {
        return rainPercent;
    }

    public void setRainPercent(double rainPercent) {
        this.rainPercent = rainPercent;
    }

    public double getTemperatureF() {
        return temperatureF;
    }

    public void setTemperatureF(double temperatureF) {
        this.temperatureF = temperatureF;
    }

    public List<ForecastEntry> getForecast() {
        return forecast;
    }

    public void setForecast(List<ForecastEntry> forecast) {
        this.forecast = forecast;
    }

    /** One forecast slot, such as "6 PM", with the wind and rain expected then. */
    public static class ForecastEntry {
        private String label;       // what the screen shows, e.g. "6 PM"
        private String time;        // ISO local time, e.g. "18:00"
        private double windMph;
        private double rainPercent;

        public ForecastEntry() {
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public double getWindMph() {
            return windMph;
        }

        public void setWindMph(double windMph) {
            this.windMph = windMph;
        }

        public double getRainPercent() {
            return rainPercent;
        }

        public void setRainPercent(double rainPercent) {
            this.rainPercent = rainPercent;
        }
    }
}
