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
import javafx.scene.control.Label;
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
        treePanel = new ItemTreePanel(root, farmController);
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

        HBox topRow = new HBox(12, alertsBox, statusBox);
        HBox.setHgrow(alertsBox, Priority.ALWAYS);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        HBox mainRow = new HBox(12, treePanel, mapPanel);
        HBox.setHgrow(mapPanel, Priority.ALWAYS);
        VBox mainBox = ScreenKit.titledBox("Farm Layout", mainRow);

        VBox actionsBox = ScreenKit.titledBox("Actions", commandPanel, droneActionPanel);

        VBox rootPane = new VBox(12, title, topRow, mainBox, actionsBox);
        rootPane.setPadding(new Insets(12));
        screen = rootPane;

        Scene scene = new Scene(rootPane, 1280, 900);
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

    public void select(FarmComponent node) {
        selected = node;
        commandPanel.showFor(node);
        statusBox.showFor(node);

    }

    public void refresh() {
        if (!built) {
            return;
        }

        if (selected == null || root.findByName(selected.getName()) == null) {
            selected = root;
        }
        treePanel.rebuild();
        mapPanel.redraw();
        alertsBox.refresh();
        statusBox.showFor(selected);
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