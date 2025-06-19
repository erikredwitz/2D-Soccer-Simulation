package model;

import java.util.ArrayList;
import java.util.List;

public class Situation {
    private List<Player> players;
    private Ball ball;
    private int redScore;
    private int blueScore;

    public Situation(List<Player> players, Ball ball, int redScore, int blueScore) {
        this.players = players;
        this.ball = ball;
        this.redScore = redScore;
        this.blueScore = blueScore;
    }

    public static Situation generateSituationFrom(List<MovingFrameElement> elements, int redScore, int blueScore) {
        List<Player> playerList = new ArrayList<>();
        Ball ball = new Ball(new TwoDimVector(0,0));
        for(var element : elements){
            if(element.isBall()){
                ball = ((Ball) element).createCopy();
                continue;
            }
            Player player = ((Player) element).createCopy();
            playerList.add(player);
        }
        return new Situation(playerList, ball, redScore, blueScore);
    }
    public List<Player> getPlayers() {
        return players;
    }

    public Ball getBall() {
        return ball;
    }

    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }
}
