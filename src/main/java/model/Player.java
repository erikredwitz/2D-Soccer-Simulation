package model;

import java.time.Duration;
import java.util.List;

import static model.Racket.RACKET_ACC_MAX;

public class Player extends MovingFrameElement{
    public static final int BODY_ACC_MAX = 1;
    public static final int PLAYER_RADIUS = 14;
    private boolean teamRed;
    private TwoDimVector bodyAcceleration; //wird bestimmt von Strategie
    private boolean letBallPass;
    private Racket racket; //racket.acceleraction wird bestimmt von Strategie

    private Strategy strategy;
    private static final double FRICTION = 5;

    @Override
    public boolean isTeamRed() {
        return teamRed;
    }

    public TwoDimVector getPosition() {
        return position;
    }

    @Override
    public boolean isBall() {
        return false;
    }


    public TwoDimVector getVelocity() {
        return velocity;
    }

    public TwoDimVector getBodyAcceleration() {
        return bodyAcceleration;
    }

    public boolean doesLetBallPass() {
        return letBallPass;
    }

    public TwoDimVector getRacketPosition() {
        return racket.getPosition();
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void decide(List<MovingFrameElement> strategyInput){
        var strategyOutput = strategy.decide(strategyInput, this);
        double bodyAccelerationStrength = strategyOutput.bodyAccelerationStrength();
        bodyAccelerationStrength = Utility.putInRange(bodyAccelerationStrength,0,1);
        final TwoDimVector bodyAccelerationDirectionNormed = strategyOutput.bodyAccelerationDirection().getNormedVector();
        bodyAcceleration = bodyAccelerationDirectionNormed.scaledWith(bodyAccelerationStrength * BODY_ACC_MAX);
        var racketAccelerationDirectionNormed = strategyOutput.racketAccelerationDirection().getNormedVector();
        var racketAccelerationStrength = strategyOutput.racketAccelerationStrength();
        racketAccelerationStrength = Utility.putInRange(racketAccelerationStrength, 0, 1);
        racket.setAcceleration(racketAccelerationDirectionNormed.scaledWith(racketAccelerationStrength * RACKET_ACC_MAX));


    }


    public TwoDimVector getRacketVelocity() {
        return racket.getVelocity();
    }

    public void moveTimeStep(Duration duration) {
        long milliseconds = duration.toMillis();
        position.add(velocity.scaledWith(milliseconds));
        velocity.add(bodyAcceleration.scaledWith(milliseconds));
        racket.moveTimeStep(duration);
    }
}
