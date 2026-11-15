package com.farmdashboard.data;
import java.util.ArrayList;
import java.util.List;


public class NodeData {
    public static final String ITEM = "ITEM";
    public static final String CONTAINER = "CONTAINER";

    private String type;
    private String name;
    private double price;
    private double x;
    private double y;
    private double length;
    private double width;
    private double height;
    private List<NodeData> child= new ArrayList<>();

    public NodeData() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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

    public List<NodeData> getChildren() {
        return child;
    }

    public void setChildren(List<NodeData> child) {
        this.child = child;
    }
}
