package com.farmdashboard.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.farmdashboard.control.DroneController;
import com.farmdashboard.control.FlightConditions;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.ReadingsData;
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

    private final Label title = new Label("Status");
    private final VBox list = new VBox(6);

    public StatusBox(ItemContainer root, DroneController droneController) {
        this.root = root;
        this.droneController = droneController;

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
            list.getChildren().add(ScreenKit.infoRow("With contents", money(node.getTotal())));
            int count = node.getChild().size();
            list.getChildren().add(ScreenKit.infoRow("Holds", count + (count == 1 ? " child" : " children")));
        }
        list.getChildren().add(ScreenKit.infoRow("Location",
                "(" + number(node.getLocationX()) + ", " + number(node.getLocationY()) + ")"));
        list.getChildren().add(ScreenKit.infoRow("Size",
                number(node.getLength()) + " x " + number(node.getWidth())
                + " x " + number(node.getHeight())));
    }

    private void addFarmTotal() {
        list.getChildren().add(ScreenKit.infoRow("Farm total", money(root.getTotal())));
    }

    private void addDrone() {
        FarmComponent drone = root.findByName("Drone");
        if (drone == null) {
            list.getChildren().add(
                    ScreenKit.infoRow("Drone", "Not on the farm", ScreenKit.Status.ACT));
            return;
        }
        double cx = drone.getCenterX();
        double cy = drone.getCenterY();
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

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);

    private void addLatestReading(FarmComponent node) {
        String text = latestReadingText(node);
        list.getChildren().add(ScreenKit.infoRow("Latest reading",
                text == null ? "None for this node" : text));
    }

    private static String latestReadingText(FarmComponent node) {
        ReadingsData data = FarmDataStore.getInstance().get(DataFile.READINGS, ReadingsData.class);
        if (data == null || data.getReadings() == null) {
            return null;
        }
        for (var reading : data.getReadings()) {
            if (reading.getItem() == null || !reading.getItem().equalsIgnoreCase(node.getName())) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            if (reading.getStatus() != null) {
                text.append(reading.getStatus());
            }
            if (reading.getLabel() != null && !reading.getLabel().isEmpty()) {
                text.append(text.length() > 0 ? ": " : "")
                    .append(reading.getLabel()).append(' ')
                    .append(reading.getPercent()).append('%');
            } else if (reading.getNote() != null && !reading.getNote().isEmpty()) {
                text.append(text.length() > 0 ? ": " : "").append(reading.getNote());
            }
            if (reading.getUpdated() != null) {
                text.append(" (").append(formatUpdated(reading.getUpdated())).append(')');
            }
            return text.toString();
        }
        return null;
    }

    private static String formatUpdated(String isoText) {
        try {
            return LocalDateTime.parse(isoText).format(TIME_FORMAT);
        } catch (RuntimeException e) {
            return isoText;
        }
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
}