package com.farmdashboard.control;

import com.farmdashboard.data.DataFile;
import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.FarmJson;
import com.farmdashboard.data.NodeData;
import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.Item;
import com.farmdashboard.entity.ItemContainer;

/**
 * One method per brief command. Each checks its input (IllegalArgumentException with a message
 * the user can read), changes the farm, then saves the whole farm to farm.json.
 */
public class FarmController {

    private final ItemContainer root;

    public FarmController(ItemContainer root) {
        this.root = root;
    }

    public Item addItem(FarmComponent parent, ComponentData data) {
        ItemContainer container = requireContainer(parent);
        validate(data);
        Item item = new Item(data.getName().trim(), data.getPrice(), data.getLocationX(),
                data.getLocationY(), data.getLength(), data.getWidth(), data.getHeight());
        container.add(item);
        save();
        return item;
    }

    public ItemContainer addContainer(FarmComponent parent, ComponentData data) {
        ItemContainer container = requireContainer(parent);
        validate(data);
        ItemContainer child = new ItemContainer(data.getName().trim(), data.getPrice(),
                data.getLocationX(), data.getLocationY(), data.getLength(), data.getWidth(),
                data.getHeight());
        container.add(child);
        save();
        return child;
    }

    /** Deletes the node and everything inside it. */
    public void delete(FarmComponent node) {
        requireNotRoot(node, "deleted");
        ItemContainer parent = node.getParent();
        if (parent == null) {
            throw new IllegalArgumentException(node.getName() + " isn't on the farm.");
        }
        parent.remove(node);
        save();
    }

    public void rename(FarmComponent node, String name) {
        requireNotRoot(node, "renamed");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name can't be blank.");
        }
        node.setName(name.trim());
        save();
    }

    public void changePrice(FarmComponent node, double price) {
        requireNotRoot(node, "given a price");
        requireAtLeastZero(price, "Price");
        node.setPrice(price);
        save();
    }

    /** Moves the node, and everything inside it, to a new x. */
    public void changeLocationX(FarmComponent node, double x) {
        requireNotRoot(node, "moved");
        requireAtLeastZero(x, "Location x");
        node.move(x, node.getLocationY());
        save();
    }

    /** Moves the node, and everything inside it, to a new y. */
    public void changeLocationY(FarmComponent node, double y) {
        requireNotRoot(node, "moved");
        requireAtLeastZero(y, "Location y");
        node.move(node.getLocationX(), y);
        save();
    }

    public void changeLength(FarmComponent node, double length) {
        requireNotRoot(node, "resized");
        requireMoreThanZero(length, "Length");
        node.setLength(length);
        save();
    }

    public void changeWidth(FarmComponent node, double width) {
        requireNotRoot(node, "resized");
        requireMoreThanZero(width, "Width");
        node.setWidth(width);
        save();
    }

    public void changeHeight(FarmComponent node, double height) {
        requireNotRoot(node, "resized");
        requireAtLeastZero(height, "Height");
        node.setHeight(height);
        save();
    }

    /** Puts every default file back, then replaces the farm with the brief's test farm and saves it. */
    public void loadTestFarm() {
        FarmDataStore store = FarmDataStore.getInstance();
        store.resetAll();
        FarmJson.copyInto(root, store.get(DataFile.TEST_FARM, NodeData.class));
        save();
    }

    /** How many items and containers are inside the node, not counting the node itself. */
    public int countContents(FarmComponent node) {
        int count = 0;
        for (FarmComponent child : node.getChild()) {
            count += 1 + countContents(child);
        }
        return count;
    }

    private void save() {
        FarmJson.saveFarm(root);
    }

    private static ItemContainer requireContainer(FarmComponent node) {
        if (node == null || !node.isContainer()) {
            throw new IllegalArgumentException("Select an item container to add to.");
        }
        return (ItemContainer) node;
    }

    private void requireNotRoot(FarmComponent node, String action) {
        if (node == null) {
            throw new IllegalArgumentException("Select an item or item container first.");
        }
        if (node == root) {
            throw new IllegalArgumentException("Root is the whole farm and can't be " + action + ".");
        }
    }

    private static void validate(ComponentData data) {
        if (data == null) {
            throw new IllegalArgumentException("No values were entered.");
        }
        if (data.getName() == null || data.getName().isBlank()) {
            throw new IllegalArgumentException("The name can't be blank.");
        }
        requireAtLeastZero(data.getPrice(), "Price");
        requireAtLeastZero(data.getLocationX(), "Location x");
        requireAtLeastZero(data.getLocationY(), "Location y");
        requireMoreThanZero(data.getLength(), "Length");
        requireMoreThanZero(data.getWidth(), "Width");
        requireAtLeastZero(data.getHeight(), "Height");
    }

    private static void requireAtLeastZero(double value, String field) {
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException(field + " can't be negative.");
        }
    }

    private static void requireMoreThanZero(double value, String field) {
        if (Double.isNaN(value) || value <= 0) {
            throw new IllegalArgumentException(field + " must be more than 0.");
        }
    }
}