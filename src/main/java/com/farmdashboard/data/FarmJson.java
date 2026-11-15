package com.farmdashboard.data;

import java.util.ArrayList;
import java.util.List;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

/**
 * Converts between the farm (the Composite) and NodeData (how it's stored in JSON),
 * and saves or loads farm.json through FarmDataStore.
 */
public final class FarmJson {

    private FarmJson() {
    }

    // Saves the whole farm to farm.json. Call it at the end of every farm command.
    public static void saveFarm(ItemContainer root) {
        FarmDataStore store = FarmDataStore.getInstance();
        store.put(DataFile.FARM, toData(root));
        store.save(DataFile.FARM);
    }

    // Replaces Root's contents with the farm saved in farm.json.
    public static void loadFarm(ItemContainer root) {
        copyInto(root, FarmDataStore.getInstance().get(DataFile.FARM, NodeData.class));
    }

    // A NodeData copy of this node and everything inside it.
    public static NodeData toData(FarmComponent node) {
        NodeData data = new NodeData();
        data.setType(node.isContainer() ? NodeData.CONTAINER : NodeData.ITEM);
        data.setName(node.getName());
        data.setPrice(node.getPrice());
        data.setX(node.getLocationX());
        data.setY(node.getLocationY());
        data.setLength(node.getLength());
        data.setWidth(node.getWidth());
        data.setHeight(node.getHeight());
        List<NodeData> children = new ArrayList<>();
        for (FarmComponent child : node.getChild()) {
            children.add(toData(child));
        }
        data.setChildren(children);
        return data;
    }

    /**
     * Replaces Root's contents with the children in data. Root keeps its own name, size and price.
     * The new tree is built before Root is cleared, so a bad file leaves the current farm as it was.
     */
    public static void copyInto(ItemContainer root, NodeData data) {
        List<FarmComponent> children = new ArrayList<>();
        for (NodeData childData : childrenOf(data)) {
            children.add(build(childData));
        }
        root.clear();
        for (FarmComponent child : children) {
            root.add(child);
        }
    }

    private static FarmComponent build(NodeData data) {
        if (data.getName() == null || data.getName().isBlank()) {
            throw new IllegalStateException("A farm node in the JSON has no name.");
        }
        if (NodeData.ITEM.equals(data.getType())) {
            if (!childrenOf(data).isEmpty()) {
                throw new IllegalStateException(
                        "Item " + data.getName() + " has children; only containers can.");
            }
            return new Item(data.getName(), data.getPrice(), data.getX(), data.getY(),
                    data.getLength(), data.getWidth(), data.getHeight());
        }
        if (NodeData.CONTAINER.equals(data.getType())) {
            ItemContainer container = new ItemContainer(data.getName(), data.getPrice(),
                    data.getX(), data.getY(), data.getLength(), data.getWidth(), data.getHeight());
            for (NodeData childData : childrenOf(data)) {
                container.add(build(childData));
            }
            return container;
        }
        throw new IllegalStateException(data.getName() + " has type \"" + data.getType()
                + "\"; it must be " + NodeData.ITEM + " or " + NodeData.CONTAINER + ".");
    }

    // The children, or an empty list when the JSON leaves them out or sets them to null.
    private static List<NodeData> childrenOf(NodeData data) {
        return data.getChildren() == null ? List.of() : data.getChildren();
    }
}
