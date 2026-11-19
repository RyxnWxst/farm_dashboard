# AGENTS.md — Farm Dashboard
 
Guide for anyone working in this repo, human or coding assistant. Read it before writing code. When the code and this file disagree, the code on `main` wins: fix this file in the same pull request.
 
## What this is
 
A JavaFX desktop app for a CS420 course project. The user builds a farm out of items and containers, sees it on an 800 × 600 map, and flies a drone over it.
 
What the brief grades:
- four use cases: **manage farm items**, **create farm layout**, **visit item** and **scan farm**;
- the **Composite** and **Singleton** patterns;
- the 800 × 600 visualization and the drone animation;
- the test scenario (the test farm below).
Anything outside that list is optional. Don't add screens, menus or use cases the brief doesn't grade.
 
## Stack and commands
 
- Java 17, JavaFX 21, Maven, Gson 2.11, JUnit 5. Base package `com.farmdashboard`, one Java module (`src/main/java/module-info.java`).
- **Run:** `mvn clean javafx:run`. In IntelliJ, use the Maven panel: Plugins → javafx → javafx:run.
  Don't run `App.main` directly: without the JavaFX module path, it fails.
- **Test:** `mvn clean test`. In IntelliJ, use the Maven panel: Lifecycle → test.
  Every test must pass before you push.
- The app writes its working data to `run-data/` in the project folder. Delete that folder to start from the defaults again.
## Layout
 
```text
src/main/java/com/farmdashboard/
├── App.java       starts JavaFX: Dashboard.getInstance().show(stage)
├── entity/        the farm model (Composite). Plain Java, no JavaFX, no JSON.
├── data/          JSON files and the classes Gson reads them into
├── control/       logic: commands, alerts, readings, flight planning. No JavaFX.
└── boundary/      everything JavaFX: window, panels, dialogs, map, animation
src/main/resources/com/farmdashboard/
├── styles.css
└── data/          the default JSON files (committed)
src/test/java/com/farmdashboard/   tests, same package as the class they test
docs/              design doc, uml/, screenshots/
run-data/          working copies of the JSON files (gitignored, never commit)
```
 
### Layer rules
 
- Calls only go **downward**: boundary → control → entity / data. Nothing in entity, data or control imports `javafx.*`.
  This keeps all the logic testable without opening a window.
- **entity** knows nothing about JSON or the screen.
- **data** knows nothing about the screen.
- Only **`FarmDataStore`** opens, reads or writes JSON files. No other class uses `Files`, `FileReader` or Gson on a data file.
- **boundary** classes don't change the farm themselves. They call a control method, and the control method saves.
## The farm model (Composite)
 
| Class | Role | Notes |
| --- | --- | --- |
| `FarmComponent` | Component (abstract) | name, price, locationX, locationY, length, width, height; parent |
| `Item` | Leaf | `getChildren()` is always empty |
| `ItemContainer` | Composite | the only class with `add`, `remove`, `clear` |
 
What you can rely on:
- **`getTotalPrice()`** returns the node's own price plus everything inside it, recursively.
- **`moveTo(x, y)`** moves the node and everything inside it by the same amount.
  There are deliberately no x/y setters: always use `moveTo`, or a container's contents get left behind.
- **`findByName(name)`** searches this node and everything inside it, ignoring case. It returns `null` if nothing matches.
- **`isInside(other)`** is true when this node's rectangle lies fully inside `other`'s rectangle.
- **`getCenterX()` / `getCenterY()`** give the drone's target point for that node.
- **`isContainer()`** tells a container from an item. Prefer it to `instanceof`.
- **`ItemContainer.add(child)`** sets the child's parent and removes the child from its old parent. It refuses to put a container inside itself.
- **`getChildren()`** on a container is read-only. Change children only through `add` / `remove`.
- **`setParent`** is package-private on purpose. Never make it public.
Code that walks the tree should use `getChildren()` recursively and work for any `FarmComponent`. Only branch on `isContainer()` when the behavior really differs, for example when only containers get the Add buttons.
 
## Data (Singleton store and JSON)
 
`FarmDataStore.getInstance()` is the single way in:
 
```java
FarmDataStore store = FarmDataStore.getInstance();
NodeData farm = store.get(DataFile.FARM, NodeData.class); // read once, same object for everyone
store.put(DataFile.FARM, newFarm);                         // replace the whole object
store.save(DataFile.FARM);                                 // write to run-data/, then run listeners
store.onChange(DataFile.FARM, this::refresh);              // redraw after every save of that file
store.resetAll();                                          // restore every default
```
 
- **`DataFile`** lists every JSON file together with its data class. To add a file:
  1. add its entry to `DataFile`;
  2. add the data class to `com.farmdashboard.data`;
  3. put the default JSON in `src/main/resources/com/farmdashboard/data/`.
- **Data classes** need a no-arg constructor and private fields with getters and setters. They hold data only, with no logic.
  They must live in `com.farmdashboard.data`: that package is the only one opened to Gson in `module-info.java`.
- **`NodeData`** is how the farm is stored. It mirrors the Composite and adds a `type` field (`"ITEM"` / `"CONTAINER"`), and it stores no parent links.
  **`FarmJson`** converts between `NodeData` and the real model. Use it rather than building `Item`s from JSON yourself.
- **Edit defaults only** in `src/main/resources/.../data/`. A change there doesn't reach a `run-data/` copy that already exists; delete `run-data/` or call `resetAll()` to pick it up.
- Check that every JSON file is valid before pushing. A broken file makes `get` throw `IllegalStateException`.
| File | Default contents | Written by |
| --- | --- | --- |
| `farm.json` | Empty farm: Root only, 800 × 600 | every farm command |
| `test-farm.json` | The brief's test scenario | never (read-only) |
| `drone.json` | Flight settings and an empty flight log | every flight |
| `weather.json` | Current conditions and forecast | never |
| `readings.json` | Latest reading per farm item | after each flight |
 
Rules that apply across files:
- `readings.json` refers to farm items by name (`"item": "Cow"`), and the code looks them up with `findByName`. Readings whose names aren't on the farm are skipped.
  A rename has to update readings too, so it goes through the store, not a bare `setName`.
- Times are ISO-8601 text. Status values are in capitals.
- Use the test farm's exact names: Soy crop, Cow, Tractor, Tiller, Milk storage, Drone.
## Design rules
 
- **Coordinates:** pixels, origin at the top left, y grows downward.
  `length` is the x extent and `width` is the y extent. `height` is stored and shown, but never drawn.
- **Root** sits at (0, 0), is 800 × 600 with price 0, can't be deleted, and can't be moved.
- **Validation:**
  - names must not be blank;
  - numbers must be ≥ 0;
  - length and width must be > 0.
  Bad input throws `IllegalArgumentException` with a message a user can read. The boundary shows that message in a dialog.
- **Moving and deleting containers:**
  - Moving a container moves everything inside it.
  - Deleting a container deletes its contents, after a confirmation that says how many items go with it.
- A node outside its parent is allowed. The map gives it a warning outline, and Alerts lists it.
- **Every farm command saves `farm.json`**, so the app reopens with the last layout.
- **One controller method per brief command:** `addItem`, `addContainer`, `delete`, `rename`, `changePrice`, `changeLocationX`, `changeLocationY`, `changeLength`, `changeWidth`, `changeHeight`.
  Don't merge them into one generic `update`.
- **The drone:**
  - The drone is the item named "Drone", in any case.
  - **Visit:** fly to the target's center at 250 px/s, hover 1 s, then fly back.
  - **Scan:** lanes 100 px apart covering x 50–750 and y 50–550, then fly home.
  - While it flies, every control is disabled and only the sprite moves.
- **Flight conditions:**
  - **Safe:** wind below 10 mph and rain below 30%.
  - **Unsafe:** wind above 20 mph, or rain 50% or more.
  - **Caution:** anything else.
  - Launch asks for confirmation when conditions are Unsafe.
- **Look:** the v3 wireframe, meaning a title bar over four titled boxes (Alerts, Status, the tree with the map, and Actions), with pill buttons.
  Build those pieces with `ScreenKit` instead of styling them by hand. Status colors: green means fine, yellow means watch, red means act now.
## The test farm (expected numbers)
 
| Name | Inside | x | y | L | W | H | Price |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Barn (container) | Root | 500 | 300 | 270 | 260 | 30 | 50,000 |
| Livestock area (container) | Barn | 520 | 320 | 140 | 110 | 10 | 5,000 |
| Cow | Livestock area | 545 | 345 | 50 | 30 | 5 | 1,500 |
| Milk storage | Barn | 520 | 460 | 120 | 70 | 12 | 8,000 |
| Storage building (container) | Root | 30 | 330 | 230 | 150 | 25 | 30,000 |
| Tractor | Storage building | 50 | 360 | 80 | 50 | 10 | 45,000 |
| Tiller | Storage building | 150 | 360 | 70 | 40 | 4 | 3,500 |
| Command center (container) | Root | 30 | 30 | 160 | 110 | 15 | 20,000 |
| Drone | Command center | 85 | 60 | 40 | 40 | 1 | 2,500 |
| Soy crop | Root | 230 | 30 | 540 | 220 | 3 | 12,000 |
 
If a change breaks any of these, it's a bug:
- **Totals:** the farm is $177,500. Barn $64,500, Storage building $78,500 and Command center $22,500, each including its contents.
- **Moving the Barn** to (450, 300) puts Livestock area at (470, 320), Cow at (495, 345) and Milk storage at (470, 460).
- **Drone and flights:**
  - The drone's center is (105, 80).
  - Visiting Cow (center 570, 360) takes 3 waypoints and about 5.3 s.
  - A scan takes 14 waypoints and about 21 s.
- **Default alerts:** Soy crop Zone 2 has low soil moisture (18%), and flying is unsafe at 6 PM. Flight conditions show Caution now (12 mph).
## Tests
 
- JUnit 5. Each test class sits in the same package as the class it tests, under `src/test/java`.
- **Name tests after the behavior they check**, for example `movingAContainerMovesItsContents()`, not `test1()`.
- **Test logic in entity, data and control.** Keep JavaFX out of tests: if something is hard to test, move the logic into control.
- **Any test that touches `FarmDataStore`** must call `store.useFolder(tempDir)` in `@BeforeEach`, with a `@TempDir Path tempDir` field.
  The store is a Singleton that outlives each test, so this resets it. It also keeps the test away from the real `run-data/`.
- **Build test farms in code** from the table above, or load them from `test-farm.json` through the store. Never read files directly.
- Compare doubles with a small delta, such as `assertEquals(570, x, 0.001)`.
## Code style
 
- Four-space indents. Braces on the same line, even for one-line `if` bodies.
- Classes are `PascalCase` nouns. Methods are `camelCase` verbs.
- Give every public class a one-line Javadoc saying what it's for. Comment the *why*, not the *what*.
- Keep fields private. Expose getters, and add setters only where something really needs them.
- Return read-only views of internal lists (`Collections.unmodifiableList`, `List.of()`).
- Throw `IllegalArgumentException` for bad input and `IllegalStateException` for "can't do that right now". Don't swallow exceptions.
- Don't use `System.out` in finished code. User-facing messages go through Alerts, Status or a dialog.
- Don't add libraries to `pom.xml` without the team agreeing first.
- Keep `module-info.java` minimal:
  - a new package under `com.farmdashboard` needs no change;
  - a package Gson must read needs an `opens ... to com.google.gson;` line.
## Git workflow (GitHub)
 
- `main` is protected. Every change goes in through a pull request that has one approval.
- Work on your own `feature/...` branch, and commit from your own GitHub account.
- Write short commit messages in the imperative, such as "Add moveTo for containers".
- **Before opening a pull request:**
  1. Merge the latest `main` into your branch.
  2. Run `mvn clean test` and make sure every test passes.
  3. Run the app, click Load Test Farm, and click through your part.
  4. Make sure nothing from `run-data/`, `target/` or IDE folders is staged.
- **Tell the team before you change a public method someone else calls.** Search for its usages first.
- **Freeze:** there is a code freeze 24 hours before the deadline. After that, only bug fixes go in, and `main` gets tagged `v1.0`.
## Working with a coding assistant
 
- Point it at this file first, and at the classes it's about to touch.
- It must follow the layer rules and the "only `FarmDataStore` touches files" rule, even if a shortcut looks easier.
- Ask it for a test with every behavior change, then run `mvn clean test` yourself. Don't trust a claim that the tests pass.
- Review every line before you commit it. You are the author, and you'll be explaining this code in the design doc.