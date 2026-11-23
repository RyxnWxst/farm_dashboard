package com.farmdashboard.boundary;

import com.farmdashboard.entity.FarmComponent;

import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The tree of items and item containers beside the map. Picking a node selects it everywhere. */
public class ItemTreePanel extends VBox {

    private final TreeView<FarmComponent> treeView = new TreeView<>();
    private boolean updating; // true while the code (not the user) changes the tree's selection

    public ItemTreePanel(FarmComponent root) {
        super(6);
        getStyleClass().add("item-tree-panel");
        Label title = new Label("Items / Item Containers");
        title.getStyleClass().add("box-title");
        treeView.setShowRoot(true);
        treeView.setPrefWidth(230);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        getChildren().addAll(title, treeView);

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (!updating && now != null) {
                Dashboard.getInstance().select(now.getValue());
            }
        });
    }

    /** Rebuilds the tree from the farm and highlights the selected node. */
    public void refresh(FarmComponent root, FarmComponent selected) {
        updating = true;
        try {
            treeView.setRoot(buildTreeItem(root));
        } finally {
            updating = false;
        }
        select(selected);
    }

    /** Highlights the node without telling Dashboard again (Dashboard is the one asking). */
    public void select(FarmComponent node) {
        TreeItem<FarmComponent> rootItem = treeView.getRoot();
        TreeItem<FarmComponent> item = rootItem == null || node == null ? null : find(rootItem, node);
        updating = true;
        try {
            if (item == null) {
                treeView.getSelectionModel().clearSelection();
            } else {
                treeView.getSelectionModel().select(item);
                treeView.scrollTo(treeView.getRow(item));
            }
        } finally {
            updating = false;
        }
    }

    private TreeItem<FarmComponent> buildTreeItem(FarmComponent component) {
        TreeItem<FarmComponent> item = new TreeItem<>(component);
        for (FarmComponent child : component.getChild()) {
            item.getChildren().add(buildTreeItem(child));
        }
        item.setExpanded(true);
        return item;
    }

    private TreeItem<FarmComponent> find(TreeItem<FarmComponent> current, FarmComponent target) {
        if (current.getValue() == target) {
            return current;
        }
        for (TreeItem<FarmComponent> child : current.getChildren()) {
            TreeItem<FarmComponent> found = find(child, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
