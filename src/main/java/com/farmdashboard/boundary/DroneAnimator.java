package com.farmdashboard.boundary;

import java.util.List;

import com.farmdashboard.control.Waypoint;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

/**
 * Plays a planned route on a DroneSprite: one straight leg per pair of waypoints at a steady
 * speed, with a pause wherever a waypoint hovers. Only the sprite moves; the Drone item stays put.
 */
public class DroneAnimator {

    private SequentialTransition flight;

    /**
     * Flies the sprite along the waypoints, starting from the first one, and runs onFinished when
     * it lands. Each leg lasts distance / speed seconds, so the whole flight takes as long as
     * FlightPlanner.seconds says. Returns the animation in case the caller wants to stop it.
     */
    public SequentialTransition fly(DroneSprite sprite, List<Waypoint> waypoints, double speed,
            Runnable onFinished) {
        if (sprite == null) {
            throw new IllegalArgumentException("There's no drone sprite to fly.");
        }
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("The flight has no waypoints.");
        }
        if (speed <= 0) {
            throw new IllegalArgumentException("The drone's speed must be more than 0.");
        }
        if (isFlying()) {
            throw new IllegalStateException("The drone is already flying.");
        }

        SequentialTransition sequence = new SequentialTransition(sprite);
        Waypoint previous = null;
        for (Waypoint waypoint : waypoints) {
            if (previous != null) {
                double distance = Math.hypot(waypoint.x() - previous.x(), waypoint.y() - previous.y());
                if (distance > 0) { // a zero-length leg would only add an empty step
                    sequence.getChildren().add(leg(previous, waypoint, distance / speed));
                }
            }
            if (waypoint.hoverSeconds() > 0) {
                sequence.getChildren().add(new PauseTransition(Duration.seconds(waypoint.hoverSeconds())));
            }
            previous = waypoint;
        }

        sequence.setOnFinished(e -> {
            flight = null;
            if (onFinished != null) {
                onFinished.run();
            }
        });
        Waypoint start = waypoints.get(0);
        sprite.moveTo(start.x(), start.y());
        flight = sequence;
        sequence.play();
        return sequence;
    }

    /** True while a flight is playing. */
    public boolean isFlying() {
        return flight != null && flight.getStatus() == Animation.Status.RUNNING;
    }

    // The child has no node of its own, so it moves the SequentialTransition's node: the sprite.
    private static TranslateTransition leg(Waypoint from, Waypoint to, double seconds) {
        TranslateTransition leg = new TranslateTransition(Duration.seconds(seconds));
        // Set both ends so every leg starts exactly where the plan says, not where the last one stopped.
        leg.setFromX(from.x());
        leg.setFromY(from.y());
        leg.setToX(to.x());
        leg.setToY(to.y());
        leg.setInterpolator(Interpolator.LINEAR); // constant speed; the default eases in and out
        return leg;
    }
}
