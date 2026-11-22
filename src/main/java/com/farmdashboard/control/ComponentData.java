package com.farmdashboard.control;

public class ComponentData {
    private String name;
    private double price;
    private double x;
    private double y;
    private double length;
    private double width;
    private double height;

    public ComponentData(String name, double price, double x, double y, 
        double length, double width, double height) {
        this.name = name;
        this.price = price;
        this.x = x;
        this.y = y;
        this.length = length;
        this.width = width;
        this.height = height;
    }
    public String getName(){
        return name;
    }

    public double getPrice(){
        return price;
    }

    public double getLocationX(){
        return x;
    }

    public double getLocationY(){
        return y;
    }

    public double getLength(){
        return length;
    }

    public double getWidth(){
        return width;
    }

    public double getHeight(){
        return height;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setPrice(double price){
        this.price = price;
    }
    public void setX(double x){
        this.x = x;
    }
    public void setY(double y){
        this.y = y;
    }
    public void setLength(double length){
        this.length = length;
    }
    public void setWidth(double width){
        this.width = width;
    }
    public void setHeight(double height){
        this.height = height;
    }
}
