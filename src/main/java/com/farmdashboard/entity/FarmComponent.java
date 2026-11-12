package com.farmdashboard.entity;
import java.util.List;
public abstract class FarmComponent {
    private String name;
    private double price;
    private double location_x;
    private double location_y;
    private double length;
    private double width;
    private double height;
    private ItemContainer parent;


    protected FarmComponent(String name, double price, double location_x, double location_y, double length, double width, double height) {
        this.name = name;
        this.price = price;
        this.location_x = location_x;
        this.location_y = location_y;
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public abstract boolean isContainer();

    public abstract List<FarmComponent> getChild();

    //Own price as well as everything inside it
    public double getTotal() {
        double total = price;
        for (FarmComponent child: getChild()) {
            total += child.getTotal();
        }
        return total;
    }
    private void translate(double x, double y) {
        location_x += x;
        location_y += y;

        for (FarmComponent child: getChild()) {
            child.translate(x,y);
        }
    }
    //Move node to certain x,y cord
    public void move(double x, double y) {
        translate(x - location_x, y - location_y);
    }

    public FarmComponent findByName(String name) {
        if (this.name.equalsIgnoreCase(name)) {
            return this;
        }
        for (FarmComponent child: getChild()) {
            FarmComponent found = child.findByName(name);
            if (found != null) {
                return found;
            }
        }
        return null;
     }
    /** True if this node's rectangle lies completely inside the other node's rectangle. */
    public boolean isInside(FarmComponent other) {
        return location_x >= other.location_x
                && location_y >= other.location_y
                && location_x + length <= other.location_x + other.length
                && location_y + width <= other.location_y + other.width;
    }

    public double getCenterX() {
        return location_x + length / 2;
    }

    public double getCenterY() {
        return location_y + width / 2;
    }

    public ItemContainer getParent() {
        return parent;
    }

    void setParent(ItemContainer parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getLocationX() {
        return location_x;
    }

    public double getLocationY() {
        return location_y;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return name;
    }
}