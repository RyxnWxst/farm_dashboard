package com.farmdashboard.boundary;

import com.farmdashboard.control.ComponentData;
import com.farmdashboard.control.FarmController;
import com.farmdashboard.entity.FarmComponent;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * The farm commands in the Actions column, one button per brief command. The buttons act on the
 * node selected in the tree or on the map; containers also get Add Item and Add Item Container.
 */
public class CommandPanel extends VBox {

    private final FarmController controller;
    private FarmComponent selected;

    private final Label target = new Label();
    private final Label hint = new Label();

    private final Button addItemBtn = button("Add Item",
            "Add a new item inside the selected item container.", this::onAddItem);
    private final Button addContainerBtn = button("Add Item Container",
            "Add a new item container inside the selected item container.", this::onAddContainer);
    private final Button renameBtn = button("Rename",
            "Change the name of the selected item or item container.", this::onRename);
    private final Button locationBtn = button("Change Location",
            "Move the selected node to a new x and y. Containers move with everything inside them.",
            this::onChangeLocation);
    private final Button priceBtn = button("Change Price",
            "Change the price of the selected node.", this::onChangePrice);
    private final Button dimensionsBtn = button("Change Dimensions",
            "Change the length, width and height of the selected node.", this::onChangeDimensions);
    private final Button deleteBtn = button("Delete",
            "Delete the selected node. A container is deleted with everything inside it.",
            this::onDelete);

    public CommandPanel(FarmController controller) {
        super(8);
        this.controller = controller;
        getStyleClass().add("command-panel");

        target.getStyleClass().add("info-label");
        target.setWrapText(true);
        hint.getStyleClass().add("info-label");
        hint.setWrapText(true);

        getChildren().addAll(target, addItemBtn, addContainerBtn, renameBtn, locationBtn,
                priceBtn, dimensionsBtn, deleteBtn, hint);
        setSelection(null);
    }

    /** Dashboard calls this whenever the selection changes. */
    public void setSelection(FarmComponent node) {
        selected = node;
        boolean none = node == null;
        boolean isRoot = !none && node.getParent() == null;
        boolean isContainer = !none && node.isContainer();

        addItemBtn.setDisable(!isContainer);
        addContainerBtn.setDisable(!isContainer);
        for (Button edit : new Button[] {renameBtn, locationBtn, priceBtn, dimensionsBtn, deleteBtn}) {
            edit.setDisable(none || isRoot);
        }

        if (none) {
            target.setText("Select an item or item container.");
            hint.setText("");
        } else if (isRoot) {
            target.setText("Commands on: Root (the whole farm)");
            hint.setText("Root is fixed at 800 x 600. Add items to it, or select another node to change it.");
        } else {
            target.setText("Commands on " + (isContainer ? "item container: " : "item: ")
                    + node.getName());
            hint.setText(isContainer ? "" : "Select an item container to add items to it.");
        }
    }

    private void onAddItem() {
        ComponentData data = InputDialogs.addItemDialog("Add Item");
        if (data != null) {
            run(() -> Dashboard.getInstance().select(controller.addItem(selected, data)));
        }
    }

    private void onAddContainer() {
        ComponentData data = InputDialogs.addItemDialog("Add Item Container");
        if (data != null) {
            run(() -> Dashboard.getInstance().select(controller.addContainer(selected, data)));
        }
    }

    private void onRename() {
        String name = InputDialogs.renameDialog(selected.getName());
        if (name != null) {
            run(() -> controller.rename(selected, name));
        }
    }

    private void onChangeLocation() {
        Double[] xy = InputDialogs.locationDialog(selected.getLocationX(), selected.getLocationY());
        if (xy != null) {
            run(() -> {
                controller.changeLocationX(selected, xy[0]);
                controller.changeLocationY(selected, xy[1]);
            });
        }
    }

    private void onChangePrice() {
        Double price = InputDialogs.priceDialog(selected.getPrice());
        if (price != null) {
            run(() -> controller.changePrice(selected, price));
        }
    }

    private void onChangeDimensions() {
        Double[] size = InputDialogs.dimensionsDialog(
                selected.getLength(), selected.getWidth(), selected.getHeight());
        if (size != null) {
            run(() -> {
                controller.changeLength(selected, size[0]);
                controller.changeWidth(selected, size[1]);
                controller.changeHeight(selected, size[2]);
            });
        }
    }

    private void onDelete() {
        int contents = controller.countContents(selected);
        if (InputDialogs.confirmDeleteDialog(selected.getName(), contents)) {
            run(() -> controller.delete(selected));
        }
    }

    // Every command reports bad input in a dialog instead of crashing.
    private static void run(Runnable command) {
        try {
            command.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            InputDialogs.showErrorDialog(e.getMessage());
        }
    }

    private static Button button(String text, String tooltip, Runnable onClick) {
        Button button = ScreenKit.pillButton(text, tooltip, onClick);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
}
