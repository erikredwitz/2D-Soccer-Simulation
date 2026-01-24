package model;

import static model.FrameCreator.HEIGHT;
import static model.Player.PLAYER_RADIUS;
import static model.Player.PLAYER_VELOCITY_PERCENTAGE_OF_RADIUS_SUM;

public class Ball extends MovingFrameElement {
    private static final double FRICTION = 0.00002 * HEIGHT;
    public static final int BALL_RADIUS = (int) (HEIGHT * 0.015);
    private static final double MAX_VELOCITY =
        (1 - PLAYER_VELOCITY_PERCENTAGE_OF_RADIUS_SUM) * Player.RADIUS_SUM;
    private boolean isInGoal = false;
    private boolean isInRedGoal = false;

    public Ball(TwoDimVector position, TwoDimVector velocity) {
        super(position, velocity);
    }

    public Ball(TwoDimVector position) {
        super(position, new TwoDimVector(0, 0));
    }

    @Override
    public boolean isBall() {
        return true;
    }

    @Override
    public boolean isTeamRed() {
        return false;
    }

    @Override
    public void setVelocity(TwoDimVector velocity) {
        if (velocity.getNorm() > MAX_VELOCITY) {
            this.velocity = velocity.getNormedVector().scaledWith(MAX_VELOCITY);
        } else {
            this.velocity = velocity;
        }
    }

    public void move() {
        position.add(velocity);
        velocity = velocity.scaledWith(1 - FRICTION);
        double cornerX = Game.PITCH_LENGTH / 2.0;
        double cornerY = Game.PITCH_WIDTH / 2.0;
        if (position.getX() < -cornerX + BALL_RADIUS || position.getX() > cornerX - BALL_RADIUS) {
            if(position.getY() < Game.getGoalYStart() || position.getY() > Game.getGoalYEnd()){
                if (position.getX() < -cornerX + BALL_RADIUS) {
                    position.setTo(-cornerX + BALL_RADIUS, position.getY());
                } else {
                    position.setTo(cornerX - BALL_RADIUS, position.getY());
                }
                velocity.setTo(velocity.getX() * -1, getVelocity().getY());
            }
            else if(position.getX() < -cornerX - BALL_RADIUS || position.getX() > cornerX + BALL_RADIUS){
                velocity.setTo(0,0);
                isInGoal = true;
                if(position.getX() * Game.RED_SIDE > 0){
                    //Ball im Tor von Team Rot
                    isInRedGoal = true;
                }
                else{
                    //Ball im Tor von Team Blau
                    isInRedGoal = false;
                }
            }
        }
        if (position.getY() < -cornerY + BALL_RADIUS) {
            position.setTo(getPosition().getX(), -cornerY + BALL_RADIUS);
            velocity.setTo(velocity.getX(), velocity.getY() * -1);
        } else if (position.getY() > cornerY - BALL_RADIUS) {
            //noinspection SuspiciousNameCombination
            position.setTo(position.getX(), cornerY - BALL_RADIUS);
            velocity.setTo(velocity.getX(), velocity.getY() * -1);
        }
    }

    public boolean isInGoal() {
        return isInGoal;
    }

    public boolean isInRedGoal() {
        return isInRedGoal;
    }

    public void setInGoal(boolean inGoal) {
        isInGoal = inGoal;
    }

    public Ball createCopy(){
      Ball copy = new Ball(position.scaledWith(1), velocity.scaledWith(1));
      copy.setId(id);
      copy.setInGoal(this.isInGoal);
      copy.isInRedGoal = this.isInRedGoal;
      return copy;
    }

    public void setInRedGoal(boolean inRedGoal) {
        isInRedGoal = inRedGoal;
    }
}
