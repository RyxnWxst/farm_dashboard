package com.farmdashboard.data;

public enum DataFile {
    FARM("farm.json", NodeData.class),            // the farm being worked on
    TEST_FARM("test-farm.json", NodeData.class),  // the brief's test scenario
    DRONE("drone.json", DroneData.class),         // flight settings and the flight log
    WEATHER("weather.json", WeatherData.class),   // current conditions and forecast
    READINGS("readings.json", ReadingsData.class); // latest reading per farm item

    private final String fileName;
    private final Class<?> dataClass;
    DataFile(String fileName, Class<?> dataClass) {
        this.fileName = fileName;
        this.dataClass = dataClass;
    }

    public String fileName() {
        return fileName;
    }

    public Class<?> dataClass() {
        return dataClass;
    }
}
