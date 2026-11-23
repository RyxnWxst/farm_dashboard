package com.farmdashboard.boundary;

import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import com.farmdashboard.control.*;
import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;

import java.util.List;

public class CommandPanel extends VBox{
    private  FarmController controller;
    private FarmComponent selected;

    private final Button addItemBtn = pill("Add Item", "Add a new item to the selected container");
    private final Button addContainerBtn = pill("Add Container", "Add a new container to the selected container");
    private final Button deleteBtn = pill("Delete", "Delete the selected component");
    private final Button renameBtn = pill("Rename", "Rename the selected component");
    private final Button priceBtn = pill("Set Price", "Set the price of the selected component");
    private final Button locationBtn = pill("Set Location", "Set the location of the selected component");
    private final Button dimensionsBtn = pill("Set Dimensions", "Set the dimensions of the selected component");

    private final Button visitBtn = pill("Visit", "Fly to the selected node and back");
    private final Button scanBtn = pill("Scan Farm", "Fly lanes over the whole farm");
    private final Button launchBtn = pill("Launch Drone", "Start the planned flight");
    private final Button loadTestBtn = pill("Load Test Farm", "Load the brief's test farm into the dashboard");

    private Button pill(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("pill-button");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    public CommandPanel(FarmController controller){
        this.controller = controller;
        getStyleClass().add("command-panel");
        Label title = new Label("Actions");
        title.getStyleClass().add("box-title");

        getChildren().addAll(title, addItemBtn, addContainerBtn, deleteBtn, renameBtn, priceBtn, locationBtn, dimensionsBtn,
                visitBtn, scanBtn, launchBtn, loadTestBtn, new Label("Drone"), new Label("Test Farm"));
       
        wireNodeCommands();
        wireDroneCommands();
        wireTestFarm();
        setSelection(null);
    }

    public void setSelection(FarmComponent selected) {
        this.selected = selected;
        boolean isContainer = selected != null && selected.isContainer();
        addItemBtn.setDisable(!isContainer);
        addContainerBtn.setDisable(!isContainer);
        deleteBtn.setDisable(selected == null);
        renameBtn.setDisable(selected == null);
        priceBtn.setDisable(selected == null);
        locationBtn.setDisable(selected == null);
        dimensionsBtn.setDisable(selected == null);

        Boolean canFly = selected != null && selected.getLocationX() >= 0 && selected.getLocationY() >= 0;
        visitBtn.setDisable(!canFly);
    }



    private void wireNodeCommands() {
        addItemBtn.setOnAction(e -> {
            ComponentData data = InputDialogs.addItemDialog();
            if (data == null) return;
            runCommand(() -> controller.addItem(selected, data));
            
        });

        addContainerBtn.setOnAction(e -> {
            ComponentData data = InputDialogs.addItemDialog();
            if (data == null) return;
            runCommand(() -> controller.addContainer(selected, data));
        });

        deleteBtn.setOnAction(e -> {
            if (selected == null) return;
            int count = controller.countComponents(selected);
            if(!InputDialogs.confirmDeleteDialog(selected.getName(), count)) return;
            runCommand(() -> controller.remove(selected.getParent(), selected));
        });

        renameBtn.setOnAction(e -> {
            if (selected == null) return;
            String newName = InputDialogs.renameDialog(selected.getName());
            if (newName == null) return;
            runCommand(() -> controller.rename(selected, newName));
        });

        priceBtn.setOnAction(e -> {
            if (selected == null) return;
            Double newPrice = InputDialogs.priceDialog(selected.getPrice());
            if (newPrice == null) return;
            runCommand(() -> controller.changePrice(selected, newPrice));
        });

        locationBtn.setOnAction(e -> {
            if (selected == null) return;
            Double[] newLocation = InputDialogs.locationDialog(selected.getLocationX(), selected.getLocationY());
            if (newLocation == null) return;
            runCommand(() -> controller.changeLocationX(selected, newLocation[0].intValue()));
            runCommand(() -> controller.changeLocationY(selected, newLocation[1].intValue()));
        });

        dimensionsBtn.setOnAction(e -> {
            if (selected == null) return;
            Double[] newDimensions = InputDialogs.dimensionsDialog(selected.getLength(), selected.getWidth(), selected.getHeight());
            if (newDimensions == null) return;
            runCommand(() -> controller.changeLength(selected, newDimensions[0]));
            runCommand(() -> controller.changeWidth(selected, newDimensions[1]));
            runCommand(() -> controller.changeHeight(selected, newDimensions[2]));
        });
    }



    private void wireDroneCommands() {
        visitBtn.setOnAction(e -> {
            if (selected == null) return;
            runCommand(() -> { 
                FarmComponent root = Dashboard.getInstance().getRoot();
                List<Waypoint> path = new DroneController().planVisit(root, selected);
                Dashboard.getInstance().flyDrone("Drone", selected, path);
            });
        });

        scanBtn.setOnAction(e -> {
            runCommand(() -> {
                FarmComponent root = Dashboard.getInstance().getRoot();
                List<Waypoint> path = new DroneController().planScan(root);
                Dashboard.getInstance().flyDrone("Drone", root, path);
            });
        });

        launchBtn.setOnAction(e -> {
            runCommand(() -> {
                FarmComponent root = Dashboard.getInstance().getRoot();
                List<Waypoint> path = new DroneController().planVisit(root, selected);
                Dashboard.getInstance().flyDrone("Drone", selected, path);
            });
        });
    }

    private void wireTestFarm() {
        loadTestBtn.setOnAction(e -> {
            runCommand(() -> {
                ItemContainer root = new ItemContainer("Root", 0, 0, 0, 800, 600, 0);
                FarmDataStore.getInstance().resetAll();
                FarmJson.loadFarm(root);
                FarmDataStore.getInstance().save(DataFile.TEST_FARM);
                Dashboard.getInstance().refresh();
            });
        });
    }



    private void runCommand(Runnable command) {
        try {
            command.run();
        } catch (IllegalArgumentException e) {
            InputDialogs.showErrorDialog(e.getMessage());
        }
    }
}   
