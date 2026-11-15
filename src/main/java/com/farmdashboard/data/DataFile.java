package com.farmdashboard.data;

public enum DataFile {
    FARM("farm.json", NodeData.class),            // the farm being worked on
    TEST_FARM("test-farm.json", NodeData.class);  // the brief's test scenario

    // Add DRONE and WEATHER (P5) and READINGS (P4) here once their data classes are on main.
    // ex: DRONE("drone.json", DroneData.class),

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
