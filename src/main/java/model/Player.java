package model;

import javax.swing.SwingUtilities;

import static model.Ball.BALL_RADIUS;
import static model.FrameCreator.HEIGHT;


public class Player extends MovingFrameElement {

    //max Spielergeschwindigkeit + max Ballgeschwindigkeit darf höchstens Spielerradius + Ballradius sein
    //maximale Ballgeschwindigkeit ist 3 mal so hoch wie die maximale Spielergeschwindigkeit
    public static final double PLAYER_VELOCITY_PERCENTAGE_OF_RADIUS_SUM = 0.25;

    public static final int PLAYER_RADIUS = (int) (0.02 * HEIGHT);
    private static final double FRICTION = 0.2;
    public static final int RADIUS_SUM = PLAYER_RADIUS + BALL_RADIUS;
    public static final double MAX_PLAYER_VELOCITY = PLAYER_VELOCITY_PERCENTAGE_OF_RADIUS_SUM * RADIUS_SUM;
    public static final double BODY_ACC_MAX = FRICTION * MAX_PLAYER_VELOCITY;
    public static final double MAX_SHOT_VELOCITY = (1 - PLAYER_VELOCITY_PERCENTAGE_OF_RADIUS_SUM) * RADIUS_SUM;


    private boolean teamRed;
    private TwoDimVector bodyAcceleration = new TwoDimVector(0, 0); //wird bestimmt von Strategie
    private volatile boolean letBallPass = true;
    private volatile TwoDimVector shotVelocity = new TwoDimVector(0, 0);
    private Strategy strategy;
    private int minimalDurationBeforeNextShot = 0;
    private int playerId; //auf Figur schreiben in der Anzeige
    static int nextPlayerId = 1;

    public Player(boolean teamRed, Strategy strategy, TwoDimVector position, int playerId) {
        super(position, new TwoDimVector(0, 0));
        this.teamRed = teamRed;
        this.strategy = strategy;
        this.playerId = playerId;

    }

    public synchronized TwoDimVector getShotVelocity() {
        return shotVelocity;
    }

    @Override
    public boolean isTeamRed() {
        return teamRed;
    }

    public synchronized TwoDimVector getPosition() {
        return position;
    }

    @Override
    public boolean isBall() {
        return false;
    }


    public synchronized TwoDimVector getVelocity() {
        return velocity;
    }

    public synchronized TwoDimVector getBodyAcceleration() {
        return bodyAcceleration;
    }

    public synchronized boolean doesLetBallPass() {
        return letBallPass;
    }


    public Strategy getStrategy() {
        return strategy;
    }

    public void updateSituation(Situation situation) {
        new Thread(() -> {
            decide(situation); // läuft im Hintergrund
        }).start();
    }

    public void decide(Situation situation) {
        var perspective = super.findMeInSituation(situation);
        if (perspective == null) {
            System.out.println("perspective == null");
            return;
        }
        Player copyOfThis = createCopy();
        var strategyOutput = strategy.decide(situation, copyOfThis);
        var newBodyAcceleration = Utility.normAndScale(strategyOutput.bodyAccelerationStrength(),
            strategyOutput.bodyAccelerationDirection(), BODY_ACC_MAX);
        var newLetBallPass = strategyOutput.letBallPass();
        var newShotVelocity = shotVelocity;
        if (!letBallPass) {
            shotVelocity = Utility.normAndScale(strategyOutput.shotStrength(), strategyOutput.shotDirection(),
                MAX_SHOT_VELOCITY);
        }
        synchronized (this) {
            bodyAcceleration = newBodyAcceleration;
            letBallPass = newLetBallPass;
            if(!letBallPass){
                shotVelocity = newShotVelocity;
            }
        }
    }

    public Player createCopy() {
        Player copyOfThis = new Player(isTeamRed(), null, position.scaledWith(1), playerId);
        copyOfThis.setId(id);
        copyOfThis.setVelocity(velocity.scaledWith(1));
        copyOfThis.setMinimalDurationBeforeNextShot(minimalDurationBeforeNextShot);
        return copyOfThis;
    }

    public synchronized void move() {
        position.add(velocity);
        if (position.getX() + Player.PLAYER_RADIUS > Game.PITCH_LENGTH / 2.0) {
            position.setTo(Game.PITCH_LENGTH / 2.0 - PLAYER_RADIUS, position.getY());
        } else if (position.getX() - Player.PLAYER_RADIUS < -Game.PITCH_LENGTH / 2.0) {
            position.setTo(-Game.PITCH_LENGTH / 2.0 + PLAYER_RADIUS, position.getY());
        }
        if (position.getY() + Player.PLAYER_RADIUS > Game.PITCH_WIDTH / 2.0) {
            position.setTo(position.getX(), Game.PITCH_WIDTH / 2.0 - PLAYER_RADIUS);
        } else if (position.getY() - Player.PLAYER_RADIUS < -Game.PITCH_WIDTH / 2.0) {
            position.setTo(position.getX(), -Game.PITCH_WIDTH / 2.0 + PLAYER_RADIUS);
        }
        velocity.add(bodyAcceleration);
        velocity = velocity.scaledWith(1 - FRICTION);
        if (minimalDurationBeforeNextShot > 0) {
            minimalDurationBeforeNextShot--;
        }
    }

    public int getMinimalDurationBeforeNextShot() {
        return minimalDurationBeforeNextShot;
    }

    public void setMinimalDurationBeforeNextShot(int minimalDurationBeforeNextShot) {
        this.minimalDurationBeforeNextShot = minimalDurationBeforeNextShot;
    }


    public synchronized void setBodyAcceleration(TwoDimVector bodyAcceleration) {
        this.bodyAcceleration = bodyAcceleration;
    }

    public TwoDimVector getKickOffPosition(boolean isOurTeamKickingOff) {
        if (isOurTeamKickingOff) {
            return new TwoDimVector(0, 0);
        } else {
            return strategy.getKickOffPosition().scaledWith((isTeamRed() ? 1.0 : -1.0) * Game.RED_SIDE);
        }
    }

    public int getPlayerId() {
        return playerId;
    }
}
