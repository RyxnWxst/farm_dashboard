package com.farmdashboard.control;

import com.farmdashboard.entity.FarmComponent;
import com.farmdashboard.entity.ItemContainer;
import com.farmdashboard.entity.Item;

import com.farmdashboard.data.FarmDataStore;
import com.farmdashboard.data.DataFile;


public class FarmController {
    private final FarmDataStore store;
    private final FarmComponent root;

    public FarmController(FarmComponent root) {
        this.root = root;
        this.store = FarmDataStore.getInstance();
    }

    private void requireContainer(FarmComponent component) {
        if (!component.isContainer()) {
            throw new IllegalArgumentException("Component is not a container: " + component.getName());
        }
    }

    private void validateData(ComponentData data) {
        if (data.getName() == null || data.getName().isEmpty()) {
            throw new IllegalArgumentException("Component name cannot be null or empty");
        }
        if (data.getPrice() < 0) {
            throw new IllegalArgumentException("Component price cannot be negative");
        }
        if (data.getLength() <= 0 || data.getWidth() <= 0 || data.getHeight() <= 0) {
            throw new IllegalArgumentException("Component dimensions must be positive");
        }
        if (data.getLocationX() < 0 || data.getLocationY() < 0) {
            throw new IllegalArgumentException("Component coordinates cannot be negative");
        }
    }

    private void save() {
        store.save(DataFile.FARM);
    }

    public void addItem(FarmComponent parent, ComponentData data){
        requireContainer(parent);
        validateData(data);
        Item item = new Item(data.getName(), data.getPrice(), data.getLocationX(), data.getLocationY(),
                data.getLength(), data.getWidth(), data.getHeight());
        ((ItemContainer) parent).add(item);
        save();
    }

    public void addContainer(FarmComponent parent, ComponentData data){
        requireContainer(parent);
        validateData(data);
        ItemContainer container = new ItemContainer(data.getName(), data.getPrice(), data.getLocationX(), data.getLocationY(),
                data.getLength(), data.getWidth(), data.getHeight());
        ((ItemContainer) parent).add(container);
        save();
    }

    public void rename(FarmComponent component, String newName) {
        if (newName == null || newName.isEmpty()) {
            throw new IllegalArgumentException("New name cannot be null or empty");
        }
        component.setName(newName);
        save();
    }

    public void changePrice(FarmComponent component, double newPrice) {
        if (newPrice < 0) {
            throw new IllegalArgumentException("New price cannot be negative");
        }
        component.setPrice(newPrice);
        save();
    }

    public void changeLocationY(FarmComponent component, int newY) {
        if (newY < 0) {
            throw new IllegalArgumentException("New Y coordinate cannot be negative");
        }
        component.setLocationY(newY);
        save();
    }

    public void changeLocationX(FarmComponent component, int newX) {
        if (newX < 0) {
            throw new IllegalArgumentException("New X coordinate cannot be negative");
        }
        component.setLocationX(newX);
        save();
    }

    public void changeLength(FarmComponent component, double newLength) {
        if (newLength <= 0) {
            throw new IllegalArgumentException("New length must be positive");
        }
        component.setLength(newLength);
        save();
    }

    public void changeWidth(FarmComponent component, double newWidth) {
        if (newWidth <= 0) {
            throw new IllegalArgumentException("New width must be positive");
        }
        component.setWidth(newWidth);
        save();
    }

    public void changeHeight(FarmComponent component, double newHeight) {
        if (newHeight <= 0) {
            throw new IllegalArgumentException("New height must be positive");
        }
        component.setHeight(newHeight);
        save();
    }

    public void remove(FarmComponent parent, FarmComponent component) {
        requireContainer(parent);
        ((ItemContainer) parent).remove(component);
        save();
    }

    public int countComponents(FarmComponent component) {
        if (component.isContainer()) {
            int count = 1; // Count the container itself
            for (FarmComponent child : ((ItemContainer) component).getChild()) {
                count += countComponents(child); // Recursively count children
            }
            return count;
        } else {
            return 1; // Count the item itself
        }
    }
}
