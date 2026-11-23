package com.farmdashboard.boundary;

import java.util.List;

import com.farmdashboard.control.DroneController;
import com.farmdashboard.control.FarmAlerts;
import com.farmdashboard.control.FarmController;
import com.farmdashboard.control.FlightConditions;
import com.farmdashboard.control.ItemReadings;
import com.farmdashboard.control.Waypoint;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Dashboard {

    private static Dashboard instance;

    private final ItemContainer root;
    private final FarmController farmController;
    private final DroneController droneController;
    private final ItemReadings itemReadings;
    private final FarmAlerts farmAlerts;

    private FarmComponent selected;
    private boolean built = false;

    private AlertsBox alertsBox;
    private StatusBox statusBox;
    private ItemTreePanel treePanel;
    private CommandPanel commandPanel;
    private VisualizationPanel mapPanel;
    private DroneActionPanel droneActionPanel;
    private DroneSprite droneSprite;
    private VBox screen; // the whole layout, disabled while the drone flies

    private Dashboard() {

        root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);

        NodeData saved = FarmDataStore.getInstance().get(DataFile.FARM, NodeData.class);
        FarmJson.copyInto(root, saved);

        farmController = new FarmController(root);
        droneController = new DroneController();
        itemReadings = new ItemReadings();
        farmAlerts = new FarmAlerts(root, itemReadings, new FlightConditions());
        selected = root;
    }

    public static synchronized Dashboard getInstance() {
        if (instance == null) {
            instance = new Dashboard();
        }
        return instance;
    }

    public void show(Stage stage) {
        if (built) {
            return;
        }
        built = true;

        alertsBox = new AlertsBox(farmAlerts);
        statusBox = new StatusBox(root, droneController, itemReadings);
        treePanel = new ItemTreePanel(root);
        mapPanel = new VisualizationPanel();
        mapPanel.setRoot(root);
        mapPanel.setOnSelect(this::select);
        droneSprite = new DroneSprite();
        droneSprite.setVisible(false); // shown only during a flight
        mapPanel.showSprite(droneSprite);
        commandPanel = new CommandPanel(farmController);
        droneActionPanel = new DroneActionPanel(droneController);

        Label title = new Label("Farm Dashboard");
        title.getStyleClass().add("title-bar");
        title.setMaxWidth(Double.MAX_VALUE);

        // Top row: Alerts and Status side by side, as in the v3 wireframe.
        HBox topRow = new HBox(12, alertsBox, statusBox);
        alertsBox.setPrefWidth(0);
        statusBox.setPrefWidth(0);
        HBox.setHgrow(alertsBox, Priority.ALWAYS);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        // Main row: the tree and the 800 x 600 map in one box, the Actions column on the right.
        Label legend = new Label("Blue = item container  |  Green = item  |  Yellow = selected  |  "
                + "Red outline = outside its container");
        legend.getStyleClass().add("info-label");
        HBox treeAndMap = new HBox(12, treePanel, mapPanel);
        VBox farmBox = ScreenKit.titledBox("Farm Items and Map (800 x 600)", treeAndMap, legend);
        HBox.setHgrow(farmBox, Priority.ALWAYS);

        Button loadTestFarm = ScreenKit.pillButton("Load Test Farm",
                "Replace the farm with the brief's test farm (worth $177,500) and save it.",
                this::loadTestFarm);
        loadTestFarm.setMaxWidth(Double.MAX_VALUE);
        VBox actionsBox = ScreenKit.titledBox("Actions", commandPanel, new Separator(),
                droneActionPanel, new Separator(), loadTestFarm);
        actionsBox.setPrefWidth(250);
        actionsBox.setMinWidth(250);

        HBox mainRow = new HBox(12, farmBox, actionsBox);

        VBox rootPane = new VBox(12, title, topRow, mainRow);
        rootPane.setPadding(new Insets(12));
        screen = rootPane;

        // Scrolls only if the window is smaller than the layout (e.g. a small laptop screen).
        ScrollPane page = new ScrollPane(rootPane);
        page.setFitToWidth(true);
        page.setFitToHeight(true);
        page.getStyleClass().add("page-scroll");

        Scene scene = new Scene(page, 1400, 940);
        scene.getStylesheets().add(
                getClass().getResource("/com/farmdashboard/styles.css").toExternalForm());

        stage.setTitle("Farm Dashboard");
        stage.setScene(scene);
        stage.show();

        FarmDataStore store = FarmDataStore.getInstance();
        for (DataFile file : DataFile.values()) {
            store.onChange(file, this::refresh);
        }

        refresh();
    }

    /** Selects a node everywhere: tree, map, Status, commands and drone panel. Null means Root. */
    public void select(FarmComponent node) {
        selected = node == null ? root : node;
        if (!built) {
            return;
        }
        treePanel.select(selected);
        commandPanel.setSelection(selected);
        statusBox.showFor(selected);
        droneActionPanel.showFor(selected);
        mapPanel.setSelected(selected);
    }

    public void refresh() {
        if (!built) {
            return;
        }
        if (!isOnFarm(selected)) {
            selected = root; // the selected node was deleted, or the farm was replaced
        }
        treePanel.refresh(root, selected);
        mapPanel.redraw();
        alertsBox.refresh();
        select(selected);
    }

    private void loadTestFarm() {
        if (!InputDialogs.confirmLoadTestFarm()) {
            return;
        }
        try {
            farmController.loadTestFarm();
            select(root);
        } catch (RuntimeException e) {
            InputDialogs.showErrorDialog("Couldn't load the test farm: " + e.getMessage());
        }
    }

    // True if the node is still part of this farm (walks up its parents to Root).
    private boolean isOnFarm(FarmComponent node) {
        for (FarmComponent n = node; n != null; n = n.getParent()) {
            if (n == root) {
                return true;
            }
        }
        return false;
    }

    public void flyDrone(String type, FarmComponent target, List<Waypoint> path) {
        double seconds = droneController.seconds(path);
        flyDrone(path, () -> {
            droneController.finishFlight(root, type, target, seconds);
            refresh();
        });
    }

    /**
     * Disables every control, flies the sprite along the waypoints, and when it lands re-enables
     * the controls and runs onLanding. Only the sprite moves; the Drone item stays where it is.
     */
    public void flyDrone(List<Waypoint> waypoints, Runnable onLanding) {
        setFlying(true);
        try {
            droneActionPanel.getAnimator().fly(droneSprite, waypoints, droneController.speed(), () -> {
                setFlying(false);
                onLanding.run();
            });
        } catch (RuntimeException e) {
            setFlying(false); // the flight never started, so don't leave the screen locked
            throw e;
        }
    }

    private void setFlying(boolean flying) {
        screen.setDisable(flying);
        droneActionPanel.setDisabledWhileFlying(flying);
        droneSprite.setVisible(flying);
    }

    public ItemContainer getRoot() { return root; }
    public FarmComponent getSelected() { return selected; }
}