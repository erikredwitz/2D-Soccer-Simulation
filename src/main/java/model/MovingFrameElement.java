package model;

import java.time.Duration;

public abstract class MovingFrameElement implements Comparable<MovingFrameElement> {
    protected TwoDimVector position;
    protected TwoDimVector velocity;

    public MovingFrameElement(TwoDimVector position, TwoDimVector velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    TwoDimVector getPosition() {
        return position;
    }

    abstract boolean isBall();

    abstract boolean isTeamRed();

    TwoDimVector getVelocity() {
        return velocity;
    }

    abstract void move();

    @Override
    public int compareTo(MovingFrameElement o) {
        if (o.isBall()) {
            return 1;
        }
        if (this.isBall()) {
            return -1;
        }
        return (int) Math.round(velocity.getNorm() - o.velocity.getNorm());
    }

    public SituationElement generateSituationElement() {
        return new SituationElement(position.scaledWith(1), velocity.scaledWith(1), isBall(), isTeamRed());
    }

    public void setVelocity(TwoDimVector velocity) {
        this.velocity = velocity;
    }

    public void setPosition(TwoDimVector position) {
        if(position.getDistanceTo(new TwoDimVector(-Game.PITCH_LENGTH/2.0,-Game.PITCH_WIDTH/2.0)) < 1){
            System.out.println("jetzt");
        }
        this.position = position;
    }
}
