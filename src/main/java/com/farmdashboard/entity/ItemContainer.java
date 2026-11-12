package com.farmdashboard.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite
 * Its children can be items or other containers. Only containers have add() and remove().
 */
public class ItemContainer extends FarmComponent {

    private final List<FarmComponent> children = new ArrayList<>();

    public ItemContainer(String name, double price, double locationX, double locationY,
                         double length, double width, double height) {
        super(name, price, locationX, locationY, length, width, height);
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    // A read-only view; change the children with add() and remove()
    @Override
    public List<FarmComponent> getChild() {
        return Collections.unmodifiableList(children);
    }

    // Adds a child and sets its parent and taking it out of its old container first
    public void add(FarmComponent child) {
        for (FarmComponent node = this; node != null; node = node.getParent()) {
            if (node == child) {
                throw new IllegalArgumentException(
                        "Can't put " + child.getName() + " inside itself or its own contents.");
            }
        }
        if (child.getParent() != null) {
            child.getParent().remove(child);
        }
        children.add(child);
        child.setParent(this);
    }

    //Removes a child and clears it's parent
    public void remove(FarmComponent child) {
        if (children.remove(child)) {
            child.setParent(null);
        }
    }

    // Removes every child
    public void clear() {
        for (FarmComponent child : new ArrayList<>(children)) {
            remove(child);
        }
    }
}
