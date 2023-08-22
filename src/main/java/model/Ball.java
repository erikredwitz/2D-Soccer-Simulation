package model;

import java.time.Duration;

public class Ball extends MovingFrameElement {
    private static final double FRICTION = 1;
    public static final int BALL_RADIUS = 7;

    public Ball(TwoDimVector position, TwoDimVector velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    public Ball() {
        this.position = new TwoDimVector(0, 0);
        this.velocity = new TwoDimVector(0, 0);
    }

    @Override
    public boolean isBall() {
        return true;
    }

    @Override
    public boolean isTeamRed() {
        return false;
    }

    public void setVelocity(TwoDimVector velocity) {
        this.velocity = velocity;
    }

    public void moveTimeStep(Duration duration) {
        position.add(velocity.scaledWith(duration.toMillis()));
    }
}
