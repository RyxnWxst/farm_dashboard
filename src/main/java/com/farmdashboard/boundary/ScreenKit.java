package com.farmdashboard.boundary;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public final class ScreenKit {
    public enum Status {
        GOOD("status-good", Color.web("#2e9e4f")),
        WATCH("status-watch", Color.web("#e0a800")),
        ACT("status-act", Color.web("#d03a3a"));

        private final String styleClass;
        private final Color color;

        Status(String styleClass, Color color) {
            this.styleClass = styleClass;
            this.color = color;
        }

        public String styleClass() { return styleClass; }
        public Color color() { return color; }
    }

    private ScreenKit() { }

    public static VBox titledBox(String title, Node... children) {
        Label heading = new Label(title);
        heading.getStyleClass().add("box-title");

        VBox body = new VBox(8, children);
        body.getStyleClass().add("box-body");
        body.setPadding(new Insets(8, 0, 0, 0));

        VBox box = new VBox(4, heading, body);
        box.getStyleClass().add("box");
        box.setPadding(new Insets(10));
        VBox.setVgrow(body, Priority.ALWAYS);
        return box;
    }

    public static Button pillButton(String text, String tooltip, Runnable onClick) {
        Button button = new Button(text);
        button.getStyleClass().add("pill-button");
        if (tooltip != null && !tooltip.isBlank()) {
            Tooltip tip = new Tooltip(tooltip);
            tip.setShowDelay(Duration.millis(300));
            tip.setWrapText(true);
            tip.setMaxWidth(280);
            button.setTooltip(tip);
        }
        if (onClick != null) {
            button.setOnAction(e -> onClick.run());
        }
        return button;
    }

    public static Button pillButton(String text, Runnable onClick) {
        return pillButton(text, null, onClick);
    }

    public static HBox alertRow(Status status, String message) {
        Circle dot = statusDot(status);

        Label text = new Label(message);
        text.getStyleClass().add("alert-text");
        text.setWrapText(true);

        HBox row = new HBox(8, dot, text);
        row.getStyleClass().addAll("alert-row", status.styleClass());
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    public static HBox infoRow(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("info-label");

        Label val = new Label(value);
        val.getStyleClass().add("info-value");
        val.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, name, spacer, val);
        row.getStyleClass().add("info-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static HBox infoRow(String label, String value, Status status) {
        HBox row = infoRow(label, value);
        row.getChildren().add(statusDot(status));
        return row;
    }

    public static Label statusLabel(String text, Status status) {
        Label label = new Label(text);
        label.getStyleClass().add(status.styleClass());
        return label;
    }

    public static Circle statusDot(Status status) {
        Circle dot = new Circle(5, status.color());
        dot.getStyleClass().add("status-dot");
        return dot;
    }

    public static Color colorOf(Status status) {
        return status.color();
    }
    
    public static Status fromWord(String word) {
        if (word == null) {
            return Status.WATCH;
        }
        switch (word.trim().toUpperCase()) {
            case "SAFE":
            case "GOOD":
            case "OK":
                return Status.GOOD;
            case "UNSAFE":
            case "LOW":
                return Status.ACT;
            case "CAUTION":
            default:
                return Status.WATCH;
        }
    }
}