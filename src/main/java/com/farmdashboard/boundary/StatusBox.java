package com.farmdashboard.boundary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.farmdashboard.control.DroneController;
import com.farmdashboard.control.FlightConditions;
import com.farmdashboard.control.ItemReadings;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.DroneData.FlightLogEntry;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.ReadingsData.Reading;
import com.farmdashboard.data.WeatherData;
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
    private final FlightConditions flights = new FlightConditions();

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

    public final void showFor(FarmComponent node) {
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
        var drone = droneController.findDrone(root);
        if (drone.isEmpty()) {
            list.getChildren().add(
                    ScreenKit.infoRow("Drone", "Not on the farm", ScreenKit.Status.ACT));
            return;
        }

        double cx = drone.get().getCenterX();
        double cy = drone.get().getCenterY();
        list.getChildren().add(ScreenKit.infoRow("Drone",
                "Home at (" + number(cx) + ", " + number(cy) + ")", ScreenKit.Status.GOOD));

        String last = flights.lastFlight()
                .map(StatusBox::describeFlight)
                .orElse("No flights yet");
        list.getChildren().add(ScreenKit.infoRow("Last flight", last));
    }

    private void addFlightConditions() {
        FlightConditions.Rating rating = droneController.conditionsNow();
        WeatherData weather =
                FarmDataStore.getInstance().get(DataFile.WEATHER, WeatherData.class);
        String text = capitalize(rating.name())
                + " (" + number(weather.getWindMph()) + " mph wind)";
        list.getChildren().add(ScreenKit.infoRow("Flight conditions", text,
                ScreenKit.fromWord(rating.name())));
    }

    private static String describeFlight(FlightLogEntry flight) {
        StringBuilder text = new StringBuilder(capitalize(flight.getType()));
        if (flight.getTarget() != null) {
            text.append(' ').append(flight.getTarget());
        }
        text.append(", ").append(number(flight.getDurationSeconds())).append(" s");
        if (flight.getStartTime() != null) {
            try {
                text.append(" (")
                    .append(formatUpdated(LocalDateTime.parse(flight.getStartTime())))
                    .append(')');
            } catch (RuntimeException e) {
                text.append(" (").append(flight.getStartTime()).append(')');
            }
        }
        return text.toString();
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);

    private void addLatestReading(FarmComponent node) {
        String text = itemReadings.latest(node.getName())
                .map(StatusBox::describe)
                .orElse("None for this node");
        list.getChildren().add(ScreenKit.infoRow("Latest reading", text));
    }

    private static String describe(Reading reading) {
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

    private static String formatUpdated(LocalDateTime time) {
        return time.format(TIME_FORMAT);
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