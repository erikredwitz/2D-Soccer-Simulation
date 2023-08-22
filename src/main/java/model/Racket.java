package model;

import java.time.Duration;

import static model.Player.PLAYER_RADIUS;

public class Racket extends MovingFrameElement {
    private TwoDimVector acceleration;
    private TwoDimVector relativePosition;
    public static final double RACKET_ACC_MAX = 2;
    private Player user;


    public Racket(Player user) {
        this.user = user;
    }

    @Override
    boolean isBall() {
        return false;
    }

    @Override
    boolean isTeamRed() {
        return false;
    }

    @Override
    void moveTimeStep(Duration durationSinceLastMovement) {
        final long milliseconds = durationSinceLastMovement.toMillis();
        this.relativePosition.add(this.velocity.scaledWith(milliseconds));
        if (relativePosition.getNorm() > PLAYER_RADIUS) {
            relativePosition = relativePosition.getNormedVector().scaledWith(PLAYER_RADIUS);
            velocity.setTo(0,0);
        } else {
            this.velocity.add(this.acceleration.scaledWith(milliseconds));
        }

    }

    public void setAcceleration(TwoDimVector acceleration) {
        this.acceleration = acceleration;
    }

}
