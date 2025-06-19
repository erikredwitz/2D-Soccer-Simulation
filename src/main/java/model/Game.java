package model;

import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static model.Ball.BALL_RADIUS;
import static model.Player.PLAYER_RADIUS;

public class Game {
    public static final int MIN_TIME_BETWEEN_FRAMES = 16;
    public static final int MINIMAL_DURATION_BEFORE_NEXT_SHOT = 3;
    public static final int PITCH_WIDTH = 440; //bestimmt die Höhe des Bildes mit
    public static final int FORBIDDEN_AREA_SIZE = PITCH_WIDTH / 2;
    public static final int PITCH_LENGTH = 1024; //bestimmt die Breite des Bildes mit
    public static final TwoDimVector LEFT_GOAL_CENTER = new TwoDimVector(-PITCH_LENGTH / 2.0, 0);
    private static final TwoDimVector RIGHT_GOAL_CENTER = new TwoDimVector(PITCH_LENGTH / 2.0, 0);
    public static final double SHOT_RANDOMNESS_MARGIN = 0.1;

    private int redScore = 0;
    private int blueScore = 0;


    private List<Player> players;
    private Ball ball;
    private FrameCreator frameCreator;

    public final static double GOAL_SIZE_PITCH_WIDTH_RATIO = 1.0 / 7;
    private static final double MAX_DISTANCE_TO_BALL_FOR_SHOT = BALL_RADIUS + (double) PLAYER_RADIUS;
    private int pointInTime;
    private List<MovingFrameElement> movingFrameElements;

    private List<Situation> history;

    public static final int RED_SIDE = -1; //red is on left side for RED_SIDE < 0. Otherwise on right side

    public Game(List<Player> players, Ball ball, FrameCreator frameCreator, boolean isTeamRedKickingOff) {
        this.players = players;
        this.ball = ball;
        this.frameCreator = frameCreator;
        movingFrameElements = new ArrayList<>(players);
        movingFrameElements.add(ball);
        history = new ArrayList<>();
        setupKickOffPosition(isTeamRedKickingOff);
    }

    private int getSide(Player player) {
        return player.isTeamRed() ? Game.RED_SIDE : -Game.RED_SIDE;
    }

    public void createAndSetFrameForPlayback(int timeInHistory) {
        if (timeInHistory <= pointInTime) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    frameCreator.createAndSetFrameForPlayback(history.get(timeInHistory), timeInHistory);
                    return null;
                }
            };
            worker.execute();
        }
    }

    public void createFrame(int timeInHistory) {
        if (timeInHistory <= pointInTime) {
            frameCreator.createFrame(history.get(timeInHistory));
        }
    }


    public void processNextTimeStep() {
        Collections.shuffle(players);
        TwoDimVector shotVelocitySum = new TwoDimVector(0, 0);
        Set<Player> playersWhoTouchTheBall = new HashSet<>();
        for (var player : players) {
            List<MovingFrameElement> listWithoutPlayer = new LinkedList<>(movingFrameElements);
            listWithoutPlayer.remove(player);
            player.decide(Situation.generateSituationFrom(listWithoutPlayer, redScore, blueScore));
            if (!player.doesLetBallPass() && canPlayerShoot(player)) {
                playersWhoTouchTheBall.add(player);
            }
        }
        if (!playersWhoTouchTheBall.isEmpty()) {
            for (var player : playersWhoTouchTheBall) {
                shotVelocitySum.add(player.getShotVelocity());
                player.setTimeOfLastShot(pointInTime);
                player.setMinimalDurationBeforeNextShot(MINIMAL_DURATION_BEFORE_NEXT_SHOT);
            }
            ball.setVelocity(shotVelocitySum.getNormedVector()
                .scaledWith(shotVelocitySum.getNorm() + ball.getVelocity().getNorm()));
            ball.setVelocity(
                ball.getVelocity().rotated(Math.random() * SHOT_RANDOMNESS_MARGIN - 0.5 * SHOT_RANDOMNESS_MARGIN));
        }

        for (var player : players) {
            player.move();
            if (isInForbiddenArea(player)) {
                player.setPosition(getClosestAllowedPosition(player));
            }
        }
        ball.move();
        pointInTime++;
        history.add(Situation.generateSituationFrom(movingFrameElements, redScore, blueScore));
        if (ball.isInGoal()) {
            boolean isTeamRedKickingOff = ball.isInRedGoal();
            setupKickOffPosition(isTeamRedKickingOff);
            if (ball.isInRedGoal()) {
                blueScore++;
            } else {
                redScore++;
            }
        }
    }

    private void setupKickOffPosition(boolean isTeamRedKickingOff) {
        ball.setPosition(new TwoDimVector(0, 0));
        ball.setInGoal(false);
        int kickingOffTeamIndex = -1;
        int otherIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (kickingOffTeamIndex == -1 && player.isTeamRed() == isTeamRedKickingOff) {
                kickingOffTeamIndex = 0;
                player.setPosition(new TwoDimVector(0, 0));
            } else {
                int index;
                int side = (player.isTeamRed() ? 1 : -1) * Game.RED_SIDE;
                int playersPerTeam = players.size() / 2;
                int playerCountWithNormalStartPosition = 0;
                if (player.isTeamRed() == isTeamRedKickingOff) {
                    index = kickingOffTeamIndex;
                    kickingOffTeamIndex++;
                    playerCountWithNormalStartPosition = playersPerTeam - 1;
                } else {
                    index = otherIndex;
                    otherIndex++;
                    playerCountWithNormalStartPosition = playersPerTeam;
                }
                player.setPosition(createStartPosition(side, index, playerCountWithNormalStartPosition));
            }
            player.setVelocity(new TwoDimVector(0, 0));
        }
    }

    private TwoDimVector getClosestAllowedPosition(Player player) {
        TwoDimVector goal;
        if (player.position.getX() < 0) {
            goal = LEFT_GOAL_CENTER;
        } else {
            goal = RIGHT_GOAL_CENTER;
        }
        return goal.plus(player.getPosition().getDifference(goal).getNormedVector()
            .scaledWith((double) FORBIDDEN_AREA_SIZE / 2 + PLAYER_RADIUS));
    }

    public static TwoDimVector createStartPosition(int side, int index, int playersPerTeam) {
        return new TwoDimVector(side * Game.PITCH_LENGTH * (index % 2 == 0 ? 1 / 8.0 : 3 / 8.0),
            -128 + (index + 1) * 256.0 / (1 + playersPerTeam));
    }

    public static TwoDimVector getGoalCenter(boolean isTeamRed) {
        if (isTeamRed && RED_SIDE < 0 || !isTeamRed && RED_SIDE >= 0) {
            return LEFT_GOAL_CENTER;
        } else {
            return RIGHT_GOAL_CENTER;
        }
    }

    private boolean isInForbiddenArea(Player player) {
        TwoDimVector ownGoal;
        TwoDimVector opponentGoal;
        boolean isTeamRed = player.isTeamRed();
        if (isTeamRed && RED_SIDE < 0 || !isTeamRed && RED_SIDE >= 0) {
            ownGoal = LEFT_GOAL_CENTER;
            opponentGoal = RIGHT_GOAL_CENTER;
        } else {
            ownGoal = RIGHT_GOAL_CENTER;
            opponentGoal = LEFT_GOAL_CENTER;
        }
        return player.getPosition().getDistanceTo(ownGoal) <
            (FORBIDDEN_AREA_SIZE / 2.0) + PLAYER_RADIUS && !isBallInForbiddenAreaOfTeam(isTeamRed) ||
            player.getPosition().getDistanceTo(opponentGoal) < FORBIDDEN_AREA_SIZE / 2.0 + PLAYER_RADIUS;
    }

    private boolean isBallInForbiddenAreaOfTeam(boolean teamRed) {
        TwoDimVector goal;
        if (teamRed && RED_SIDE < 0 || !teamRed && RED_SIDE >= 0) {
            goal = LEFT_GOAL_CENTER;
        } else {
            goal = RIGHT_GOAL_CENTER;
        }
        //noinspection IntegerDivisionInFloatingPointContext
        return ball.getPosition().getDistanceTo(goal) <
            FORBIDDEN_AREA_SIZE / 2 + BALL_RADIUS;
    }


    private boolean canPlayerShoot(Player player) {
        boolean timing = player.getMinimalDurationBeforeNextShot() + player.getTimeOfLastShot() <= pointInTime;
        boolean distance = player.getPosition().getDistanceTo(ball.getPosition()) <= MAX_DISTANCE_TO_BALL_FOR_SHOT;
        return timing && distance;
    }

    public int getPointInTime() {
        return pointInTime;
    }

    public static int getGoalYStart() {
        return (int) (-GOAL_SIZE_PITCH_WIDTH_RATIO * PITCH_WIDTH / 2);
    }

    public static int getGoalYEnd() {
        return -getGoalYStart();
    }


}

