package com.farmdashboard.entity;

import java.util.List;

// the Leaf component
public class Item extends FarmComponent {

    public Item(String name, double price, double locationX, double locationY,
                double length, double width, double height) {
        super(name, price, locationX, locationY, length, width, height);
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public List<FarmComponent> getChild() {
        return List.of();
    }
}
