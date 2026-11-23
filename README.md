# Farm Dashboard

A desktop application using JavaFX to control a drone on a farm.

Using nodes the farmer can create their farm and view it on an 800 x 600 scale. The drone can then be flown to any node and/or used to scan the farm. The farm will be saved automatically, and the last farm will be loaded upon restarting.

**Repository:** https://github.com/RyxnWxst/farm_dashboard

## Features

- **Farm Components:** items and item containers can be added, removed and edited (price, x/y co-ordinates and length/width/height). Wherever a container is removed or relocated, all items and containers within it are too.
- **Farm Structure:** items and containers can be nested to any level (for example Barn → Livestock area → Cow).
- **Visualization:** the farm can be viewed on an 800x600 scale, where 1 pixel = 1 unit. Nodes can be clicked on both the map and tree to select them. Those that are not contained by another will be highlighted in red and a warning shown.
- **Visit Item:** the drone is flown from the Command centre to the selected node. It then hovers and returns.
- **Scan Farm:** the drone scans the farm, 100 px apart in rows 50px from the sides. Finally, it returns to the Command centre.
- **Warnings and Status:** Warnings displays information such as uncontained nodes, absence of drone, low soil moisture and upcoming unsafe flying. Status provides details including the currently selected node, farm value, drone details, flying conditions, last flight information and most recent data for the selected node.
- **Flight Conditions:** information on current flying conditions is gained from `weather.json`. When trying to fly whilst in Unsafe conditions, the user is prompted for confirmation.

## Requirements

- JDK version 17 or above
- Maven (version 3.8 or above included in IntelliJ IDEA and Eclipse). JavaFX 21 and libraries Gson and JUnit 5 are automatically downloaded.

## How to run

- **IntelliJ:** open the project. Open the Maven tool window. Navigate to **Plugins → javafx → javafx:run**.
- **Eclipse:**
    1. Make sure JDK 17 or above is set under **Window → Preferences → Java → Installed JREs**.
    2. Import the project with **File → Import → Maven → Existing Maven Projects**. Browse to the project directory (the one containing `pom.xml`) and click **Finish**. Eclipse downloads the dependencies.
    3. Right-click the project and choose **Run As → Maven build…**. Enter `clean javafx:run` in **Goals** and click **Run**. Eclipse saves this as a run configuration, so next time choose it from **Run As → Maven build**.
- **Terminal:** within the project directory,
  ```
  mvn clean javafx:run
  ```

Run `javafx:run` rather than directly running `App.main` (in Eclipse, don't use **Run As → Java Application**). Maven adds JavaFX to the module path. Simply running the program would result in “JavaFX runtime components are missing”.

## How to run the tests

- **IntelliJ:** open the Maven tool window. Then **Lifecycle → test**.
- **Eclipse:** right-click the project and choose **Run As → Maven test**. The results appear in the Console view.
- **Terminal:**
  ```
  mvn clean test
  ```

There are 113 JUnit 5 tests for:
- Farm Components – Composite;
- Data handling – data store, together with save and load functionality;
- Test farm;
- Farm commands;
- Data;
- Flight planning and flying conditions;
- Drone control.

## Dashboard operation

1. Confirm by clicking ‘Yes’ after clicking **Load Test Farm** at the bottom of the Actions column. This loads the test farm defined in the brief. Its value is $177,500.
2. Click on either a node in the **Items / Item Containers** tree or on the map. The relevant commands will then appear in the Actions column.
3. **Add Item** and **Add Item Container** insert new components into the currently selected container. Click Root to add to the farm.
4. **Rename**, **Change Location**, **Change Price** and **Change Dimensions** and **Delete** edit the selected node. The Root cannot be edited, relocated or deleted; it is always 800 x 600.
5. From **Drone Actions**, select **Visit Item / Item Container** or **Scan Farm**. Then click **Launch Drone**. All buttons are disabled until the drone has landed.

Blue represents a container, green is for an item. Yellow indicates a selected node. Red dashed lines highlight a node not contained in another.

### Test farm

The top-left hand corner is the origin, in pixels.

| Name | Inside | x | y | Length | Width | Height | Price |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Barn (container) | Root | 500 | 300 | 270 | 260 | 30 | 50,000 |
| Livestock area (container) | Barn | 520 | 320 | 140 | 110 | 10 | 5,000 |
| Cow | Livestock area | 545 | 345 | 50 | 30 | 5 | 1,500 |
| Milk storage | Barn | 520 | 460 | 120 | 70 | 12 | 8,000 |
| Storage building (container) | Root | 30 | 330 | 230 | 150 | 25 | 30,000 |
| tractor | Storage building | 50 | 360 | 80 | 50 | 10 | 45,000 |
| Tiller | Storage building | 150 | 360 | 70 | 40 | 4 | 3,500 |
| Command centre (container) | Root | 30 | 30 | 160 | 110 | 15 | 20,000 |
| Drone | Command centre | 85 | 60 | 40 | 40 | 1 | 2,500 |
| Soy crop | Root | 230 | 30 | 540 | 220 | 3 | 12,000 |

Expected outputs:
- **Value:** the farm is valued at $177,500. Contained within the Barn is a value of $64,500, the Storage building is valued at $78,500, and the Command centre at $22,500.
- **Relocating the Barn:** if the Barn is relocated to (450, 300), then the Livestock area is at (470, 320), the Cow at (495, 345) and Milk storage at (470, 460).
- **Time:** visiting the Cow takes around 5.3 seconds. Scanning the farm takes approximately 20.9 seconds (distance scanned: 5,236px; speed of drone: 250px/s).

## Data

Five JSON files store the dashboard’s data. Only `FarmDataStore` accesses and modifies these.

| File | Contents | Updated when |
| --- | --- | --- |
| `farm.json` | The current farm (initially containing just a Root node) | Farm commands |
| `test-farm.json` | Brief test farm | Not updated (read-only) |
| `drone.json` | Drone speed, hovering duration and scan spacing; together with flight history | Each flight |
| `weather.json` | Present and forecasted wind, rain and temperature | Not updated |
| `readings.json` | Most recent data for all items: soil moisture regions, livestock and equipment status; storage volume and drone battery | After flights - timestamp is updated for the data of visited items |

- **Default data** is stored in `src/main/resources/com/farmdashboard/data/` and is version-controlled. Amend this to modify the initial data.
- **Working data** is held within the `run-data/` directory in the project. This directory is generated on first running of the application and is never version-controlled.
- **To restore default data,** either exit the dashboard and remove the `run-data/` directory or click **Load Test Farm**. This restores all default files and then loads the test farm.

Artificial data is used for both weather and data. This simulates real time weather data and drone sensor data.

## Design

- **Composite:** `FarmComponent` is an abstract class. `Item` is a leaf, and `ItemContainer` is a composite. Methods `getTotal()`, `move()` and `findByName()` traverse the structure. Therefore a container includes the price, position and search results for all its internal components.
- **Singleton:** both `Dashboard` (single window) and `FarmDataStore` (single data file instance) use a private constructor alongside a `synchronized getInstance()` method.
- **Entity-Control-Boundary:** all classes are in the `com.farmdashboard` package. They are separated into:
    - `entity` – farm model;
    - `data` – JSON files;
    - `control` – commands, warnings, data and flight control. JavaFX is not used here;
    - `boundary` – visual elements.

  Communication is in a downwards direction. Consequently, all logic can be tested without needing to run the window.
- **UML class diagrams** can be found in [`docs/uml/`](docs/uml/). There is one for the system as a whole and also one for each team member’s area of responsibility.

```
src/main/java/com/farmdashboard/
├── App.java       starts JavaFX
├── entity/        FarmComponent, Item, ItemContainer
├── data/          FarmDataStore, DataFile, FarmJson, NodeData, DroneData, WeatherData, ReadingsData
├── control/       FarmController, ComponentData, FarmAlerts, ItemReadings,
│                  DroneController, FlightPlanner, FlightConditions, Waypoint
└── boundary/      Dashboard, ScreenKit, AlertsBox, StatusBox, ItemTreePanel, CommandPanel,
                   InputDialogs, VisualizationPanel, DroneActionPanel, DroneAnimator, DroneSprite
```
