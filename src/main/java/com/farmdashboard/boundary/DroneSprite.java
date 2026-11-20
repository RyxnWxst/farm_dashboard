package com.farmdashboard.boundary;

import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * The little quadcopter that flies over the map: about 24 px across, centered on its position.
 * It lives on VisualizationPanel's overlay, which lines up with the map, so its translate x/y are
 * map coordinates. It's only a picture: moving it never moves the Drone item on the farm.
 */
public class DroneSprite extends Group {

    public static final double SIZE = 24;

    private static final double ARM = 8;         // body center to rotor center, along each axis
    private static final double ROTOR_RADIUS = 4; // ARM + ROTOR_RADIUS = SIZE / 2
    private static final double BODY = 8;

    public DroneSprite() {
        getStyleClass().add("drone-sprite");
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                Line arm = new Line(0, 0, sx * ARM, sy * ARM);
                arm.getStyleClass().add("drone-arm");
                Circle rotor = new Circle(sx * ARM, sy * ARM, ROTOR_RADIUS);
                rotor.getStyleClass().add("drone-rotor");
                getChildren().addAll(arm, rotor);
            }
        }
        Rectangle body = new Rectangle(-BODY / 2, -BODY / 2, BODY, BODY);
        body.getStyleClass().add("drone-body");
        getChildren().add(body); // added last so it sits on top of the arms
        setMouseTransparent(true); // a flying drone shouldn't steal clicks from the map
    }

    /** Puts the sprite's center at (x, y) on the map. */
    public void moveTo(double x, double y) {
        setTranslateX(x);
        setTranslateY(y);
    }

    /** The sprite's center on the map, x. */
    public double getMapX() {
        return getTranslateX();
    }

    /** The sprite's center on the map, y. */
    public double getMapY() {
        return getTranslateY();
    }
}
