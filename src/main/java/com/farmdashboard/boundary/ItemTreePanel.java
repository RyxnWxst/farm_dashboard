package com.farmdashboard.boundary;

import javafx.scene.layout.VBox;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.Label;

import com.farmdashboard.entity.FarmComponent;

public class ItemTreePanel extends VBox {
    private final TreeView<FarmComponent> treeView = new TreeView<>();

    private TreeItem<FarmComponent> buildTreeItem(FarmComponent component) {
        TreeItem<FarmComponent> item = new TreeItem<>(component);
        if (component.isContainer()) {
            for (FarmComponent child : component.getChild()) {
                item.getChildren().add(buildTreeItem(child));
            }
            item.setExpanded(true);
        }
        return item;
    }

    private FarmComponent selectedNode() {
        TreeItem<FarmComponent> selectedItem = treeView.getSelectionModel().getSelectedItem();
        return selectedItem != null ? selectedItem.getValue() : null;
    }

    private void reselect(FarmComponent root, FarmComponent target) {
        TreeItem<FarmComponent> rootItem = treeView.getRoot();
        if (rootItem != null) {
            TreeItem<FarmComponent> targetItem = findTreeItem(rootItem, target);
            if (targetItem != null) {
                treeView.getSelectionModel().select(targetItem);
            }
        }
    }

    private TreeItem<FarmComponent> findTreeItem(TreeItem<FarmComponent> current, FarmComponent target) {
        if (current.getValue() == target) {
            return current;
        }
        for (TreeItem<FarmComponent> child : current.getChildren()) {
            TreeItem<FarmComponent> found = findTreeItem(child, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
    
    public ItemTreePanel(FarmComponent root) {
        getStyleClass().add("item-tree-panel");
        Label title = new Label("Farm Components");
        title.getStyleClass().add("box-title");
        treeView.setShowRoot(true);
        treeView.setPrefWidth(260);
        getChildren().addAll(title, treeView);

        treeView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                FarmComponent selectedComponent = newSelection.getValue();
                Dashboard.getInstance().select(selectedComponent);
            }
        });
    }

    public void refresh (FarmComponent root) {
        FarmComponent selected = selectedNode();
        treeView.setRoot(buildTreeItem(root));
        if (selected != null) {
            reselect(root, selected);
        }

    }



}
