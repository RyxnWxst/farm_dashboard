package com.farmdashboard.boundary;

import java.util.List;

import com.farmdashboard.control.FarmAlerts;
import com.farmdashboard.control.FarmAlerts.Alert;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AlertsBox extends VBox {

    private final FarmAlerts farmAlerts;
    private final Label title = new Label("Alerts");
    private final VBox list = new VBox(6);

    public AlertsBox(FarmAlerts farmAlerts) {
        this.farmAlerts = farmAlerts;

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
        scroll.getStyleClass().add("alerts-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);

        refresh();
    }

    public final void refresh() {
        List<Alert> alerts = farmAlerts.getAlerts();

        list.getChildren().clear();
        title.setText(alerts.isEmpty() ? "Alerts" : "Alerts (" + alerts.size() + ")");

        if (alerts.isEmpty()) {
            list.getChildren().add(
                    ScreenKit.alertRow(ScreenKit.Status.GOOD, "No alerts. Everything looks fine."));
            return;
        }

        for (Alert alert : alerts) {
            list.getChildren().add(ScreenKit.alertRow(toStatus(alert), alert.message()));
        }
    }

    private static ScreenKit.Status toStatus(Alert alert) {
        switch (alert.severity()) {
            case ACT:
                return ScreenKit.Status.ACT;
            case WATCH:
            default:
                return ScreenKit.Status.WATCH;
        }
    }
}
