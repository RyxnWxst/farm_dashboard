package com.farmdashboard.boundary;

import java.util.Locale;

import com.farmdashboard.control.DroneController;
import com.farmdashboard.control.FlightConditions;
import com.farmdashboard.control.ItemReadings;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StatusBox extends VBox {

    private final ItemContainer root;
    private final DroneController droneController;
    private final ItemReadings itemReadings;

    private final Label title = new Label("Status");
    private final VBox list = new VBox(6);

    public StatusBox(ItemContainer root, DroneController droneController,
            ItemReadings itemReadings) {
        this.root = root;
        this.droneController = droneController;
        this.itemReadings = itemReadings;

        getStyleClass().add("box");
        setPadding(new Insets(10));
        setSpacing(4);

        title.getStyleClass().add("box-title");

        list.getStyleClass().add("box-body");
        list.setPadding(new Insets(8, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(110);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("status-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);

        showFor(root);
    }

    public void showFor(FarmComponent node) {
        FarmComponent shown = (node == null) ? root : node;
        list.getChildren().clear();

        addNodeDetails(shown);
        addFarmTotal();
        addDrone();
        addFlightConditions();
        addLatestReading(shown);
    }

    private void addNodeDetails(FarmComponent node) {
        title.setText("Status: " + node.getName());

        String kind = node.isContainer() ? "Item container" : "Item";
        list.getChildren().add(ScreenKit.infoRow("Selected", node.getName() + " (" + kind + ")"));
        list.getChildren().add(ScreenKit.infoRow("Price", money(node.getPrice())));
        if (node.isContainer()) {
            list.getChildren().add(ScreenKit.infoRow("With contents", money(node.getTotalPrice())));
            int count = ((ItemContainer) node).getChildren().size();
            list.getChildren().add(ScreenKit.infoRow("Holds", count + (count == 1 ? " child" : " children")));
        }
        list.getChildren().add(ScreenKit.infoRow("Location",
                "(" + number(node.getX()) + ", " + number(node.getY()) + ")"));
        list.getChildren().add(ScreenKit.infoRow("Size",
                number(node.getLength()) + " x " + number(node.getWidth())
                + " x " + number(node.getHeight())));
    }

    private void addFarmTotal() {
        list.getChildren().add(ScreenKit.infoRow("Farm total", money(root.getTotalPrice())));
    }

    private void addDrone() {
        FarmComponent drone = findIgnoreCase(root, "Drone");
        if (drone == null) {
            list.getChildren().add(
                    ScreenKit.infoRow("Drone", "Not on the farm", ScreenKit.Status.ACT));
            return;
        }
        double cx = drone.getX() + drone.getLength() / 2.0;
        double cy = drone.getY() + drone.getWidth() / 2.0;
        list.getChildren().add(ScreenKit.infoRow("Drone",
                "Home at (" + number(cx) + ", " + number(cy) + ")", ScreenKit.Status.GOOD));

        String last = droneController.getLastFlightText();
        list.getChildren().add(ScreenKit.infoRow("Last flight",
                last == null ? "No flights yet" : last));
    }

    private void addFlightConditions() {
        FlightConditions conditions = droneController.getFlightConditions();
        if (conditions == null) {
            return;
        }
        String word = conditions.nowStatus();
        String text = capitalize(word) + " (" + number(conditions.nowWindMph()) + " mph wind)";
        list.getChildren().add(ScreenKit.infoRow("Flight conditions", text, ScreenKit.fromWord(word)));
    }

    private void addLatestReading(FarmComponent node) {
        String reading = itemReadings.describe(node);
        list.getChildren().add(ScreenKit.infoRow("Latest reading",
                reading == null ? "None for this node" : reading));
    }

    private static String money(double value) {
        return String.format(Locale.US, "$%,.0f", value);
    }

    private static String number(double value) {
        return (value == Math.rint(value))
                ? String.format(Locale.US, "%d", (long) value)
                : String.format(Locale.US, "%.1f", value);
    }

    private static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return word.substring(0, 1).toUpperCase(Locale.US) + word.substring(1).toLowerCase(Locale.US);
    }

    private static FarmComponent findIgnoreCase(FarmComponent node, String name) {
        if (node.getName().equalsIgnoreCase(name)) {
            return node;
        }
        if (node.isContainer()) {
            for (FarmComponent child : ((ItemContainer) node).getChildren()) {
                FarmComponent found = findIgnoreCase(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
