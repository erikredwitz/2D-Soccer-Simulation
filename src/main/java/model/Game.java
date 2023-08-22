package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static model.FrameCreator.BALL_RADIUS;
import static model.FrameCreator.PLAYER_RADIUS;

public class Game {
    public static final int MIN_TIME_BETWEEN_FRAMES = 16;
    private List<Player> players;
    private Ball ball;
    private FrameCreator frameCreator;
    private static final double MAX_DISTANCE_TO_BALL_FOR_SHOT = BALL_RADIUS + PLAYER_RADIUS;

    public Game(List<Player> players, Ball ball, FrameCreator frameCreator) {
        this.players = players;
        this.ball = ball;
        this.frameCreator = frameCreator;
    }

    public synchronized void playGame() {
        List<MovingFrameElement> movingFrameElements = new ArrayList<>(players);
        movingFrameElements.add(ball);
        frameCreator.createFrame(movingFrameElements);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime lastFrame = start;
        LocalDateTime lastMoveTime = start;
        while (Duration.between(start, lastFrame).toMinutes() < 1) {
            Duration duration = Duration.between(lastFrame, LocalDateTime.now());
            long millis = duration.toMillis();
            while (millis < MIN_TIME_BETWEEN_FRAMES) {
                try {
                    wait(16 - millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                duration = Duration.between(lastFrame, LocalDateTime.now());
                millis = duration.toMillis();
            }
            Collections.shuffle(players);
            for (var player : players) {
                player.decide(movingFrameElements);
                if(player.doesLetBallPass() && player.getRacketPosition().getDistanceTo(ball.position) <= BALL_RADIUS){
                    var racketVelocity = player.getRacketVelocity();
                    ball.velocity = racketVelocity.getNormedVector().scaledWith(racketVelocity.getNorm() + ball.velocity.getNorm());//hier weitermachen
                }
            }
            var thisMoveTime = LocalDateTime.now();

            //Wie umgehen mit gleichzeitigen Ballkontakten?
            //Schussvektoren addieren. aber der Ball behält seine Geschwindigkeitsnorm und dreht die Richtung der Geschwindigkeit um
            //und kriegt noch die extra Geschwindigkeit vom Schuss addiert.
            //wenn er einem entgegenkommt und man schießt.
            //Aber bei einem Presschlag wird der Ball gestoppt und behält seine Geschwindigkeit nicht bei,
            //anders als wenn man auf den bisherige Geschwindigkeitsbetrag 0 addieren würde. (0 weil sich die 2 Schüsse vom Pressschlag gegenseitig aufheben)
            //oder es kann pro Runde nur einer Schießen (der hat ja dann auch cooldown, die anderen nicht)
            //hier mache ich es jetzt aber so, dass es einfach addiert wird:
            //entgegengesetzt gerichtete gleichzeitige gleich starke Kontakte heben sich gegenseitig auf

            for (var player : players) {
                player.moveTimeStep(Duration.between(lastMoveTime, thisMoveTime));
            }
            ball.moveTimeStep(duration);
            lastMoveTime = thisMoveTime;
            frameCreator.createFrame(movingFrameElements);
            lastFrame = LocalDateTime.now();
        }
    }
}
