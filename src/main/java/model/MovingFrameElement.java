package model;

import java.time.Duration;

public abstract class MovingFrameElement implements Comparable<MovingFrameElement>{
    protected TwoDimVector position;
    protected TwoDimVector velocity;
    TwoDimVector getPosition(){
        return position;
    }
    abstract boolean isBall();
    abstract boolean isTeamRed();

    TwoDimVector getVelocity(){
        return velocity;
    }
    abstract void moveTimeStep(Duration durationSinceLastMovement);

    @Override
    public int compareTo(MovingFrameElement o) {
        if(o.isBall()){
            return 1;
        }
        if(this.isBall()){
            return -1;
        }
        return (int) Math.round(velocity.getNorm() - o.velocity.getNorm());
    }
}
