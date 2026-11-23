package com.farmdashboard.boundary;

import java.util.List;
import java.util.Optional;

import com.farmdashboard.control.DroneController;
import com.farmdashboard.control.FlightConditions.Rating;
import com.farmdashboard.control.Waypoint;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * The drone part of the Actions box: choose Visit or Scan, then Launch Drone. It checks the
 * choice, confirms an UNSAFE launch, plans the route with DroneController and hands it to
 * Dashboard.flyDrone. It never changes the farm or moves the Drone item itself.
 */
public class DroneActionPanel extends VBox {

    private final DroneController droneController;
    private final DroneAnimator animator = new DroneAnimator();

    private final RadioButton visitChoice = new RadioButton("Visit Item / Item Container");
    private final RadioButton scanChoice = new RadioButton("Scan Farm");
    private final Label target = new Label();
    private final Button launchButton;
    private final Tooltip launchTip = new Tooltip();

    private FarmComponent selected;
    private boolean flying;

    public DroneActionPanel(DroneController droneController) {
        super(8);
        this.droneController = droneController;
        getStyleClass().add("drone-actions");
        setAlignment(Pos.TOP_LEFT);

        ToggleGroup mode = new ToggleGroup();
        visitChoice.setToggleGroup(mode);
        scanChoice.setToggleGroup(mode);
        visitChoice.setSelected(true);
        mode.selectedToggleProperty().addListener((obs, old, now) -> updateLaunch());

        launchButton = ScreenKit.pillButton("Launch Drone", this::launch);
        launchButton.setMaxWidth(Double.MAX_VALUE);

        // A disabled button gets no mouse events, so its own tooltip would never show the reason.
        // The tooltip goes on a wrapper around the button instead.
        StackPane launchHolder = new StackPane(launchButton);
        launchHolder.setMaxWidth(Double.MAX_VALUE);
        launchTip.setShowDelay(Duration.millis(300));
        launchTip.setWrapText(true);
        launchTip.setMaxWidth(280);
        Tooltip.install(launchHolder, launchTip);

        Label heading = new Label("Drone Actions");
        heading.getStyleClass().add("box-title");
        target.getStyleClass().add("info-label");
        target.setWrapText(true);
        getChildren().addAll(heading, visitChoice, scanChoice, launchHolder, target);
        updateLaunch();
    }

    /** Dashboard calls this when the selection changes, so Launch can say whether Visit is possible. */
    public void showFor(FarmComponent node) {
        selected = node;
        updateLaunch();
    }

    /** Locks every drone control while the sprite is in the air, and unlocks them when it lands. */
    public void setDisabledWhileFlying(boolean flying) {
        this.flying = flying;
        visitChoice.setDisable(flying);
        scanChoice.setDisable(flying);
        updateLaunch();
    }

    /** The animator Dashboard.flyDrone uses to move the sprite. */
    public DroneAnimator getAnimator() {
        return animator;
    }

    private void updateLaunch() {
        String problem = flying ? "The drone is already flying." : visitProblem();
        launchButton.setDisable(problem != null);
        if (problem != null) {
            launchTip.setText(problem);
        } else if (visitChoice.isSelected()) {
            launchTip.setText("Fly to " + selected.getName() + ", hover, and come back.");
        } else {
            launchTip.setText("Fly lanes over the whole farm, then come back.");
        }
        target.setText(flying ? "The drone is flying..." : problem != null ? problem
                                                           : visitChoice.isSelected() ? "Target: " + selected.getName() : "Target: the whole farm");
    }

    // Why Visit can't launch right now, or null if it can (or Scan is chosen).
    private String visitProblem() {
        if (!visitChoice.isSelected()) {
            return null;
        }
        if (selected == null) {
            return "Select an item on the map or in the tree to visit.";
        }
        ItemContainer root = Dashboard.getInstance().getRoot();
        if (selected == root) {
            return "Root is the whole farm. Select an item to visit, or choose Scan farm.";
        }
        Optional<FarmComponent> drone = droneController.findDrone(root);
        if (drone.isPresent() && drone.get() == selected) {
            return "The drone can't visit itself. Select another item.";
        }
        return null;
    }

    private void launch() {
        if (flying || launchButton.isDisabled()) {
            return;
        }
        try {
            if (droneController.conditionsNow() == Rating.UNSAFE && !confirmUnsafeLaunch()) {
                return;
            }
            Dashboard dashboard = Dashboard.getInstance();
            ItemContainer root = dashboard.getRoot();
            if (visitChoice.isSelected()) {
                List<Waypoint> path = droneController.planVisit(root, selected);
                dashboard.flyDrone(FlightLogEntry.VISIT, selected, path);
            } else {
                List<Waypoint> path = droneController.planScan(root);
                dashboard.flyDrone(FlightLogEntry.SCAN, null, path);
            }
        } catch (RuntimeException e) {
            // e.g. no drone on the farm, or a broken data file; the user sees the message.
            showError(e.getMessage());
        }
    }

    private boolean confirmUnsafeLaunch() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Flight conditions are UNSAFE right now (wind over 20 mph or rain 50% or more).\n"
                        + "Launch the drone anyway?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Unsafe flying conditions");
        confirm.setHeaderText("Unsafe to fly");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK)).setText("Launch anyway");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK)).setDefaultButton(false);
        return confirm.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private static void showError(String message) {
        Alert error = new Alert(Alert.AlertType.ERROR,
                message == null || message.isBlank() ? "The drone couldn't launch." : message,
                ButtonType.OK);
        error.setTitle("Can't launch the drone");
        error.setHeaderText("Can't launch the drone");
        error.showAndWait();
    }
}