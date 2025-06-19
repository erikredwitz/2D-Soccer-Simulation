package view;

import model.Ball;
import model.FrameCreator;
import model.Game;
import model.Player;
import model.ScriptedStrategy;
import model.TwoDimVector;

import java.awt.EventQueue;
import java.util.LinkedList;
import java.util.List;

public class Application {
    public static void main(String[] args) {
        var gui = new GUI();
        EventQueue.invokeLater(() -> gui.setVisible(true));
        FrameCreator frameCreator = new FrameCreator(gui);
        Game game = createNewGameWithBots(5, frameCreator);
        gui.setGame(game);
        game.processNextTimeStep();
        game.createAndSetFrameForPlayback(0);
    }

    private static Game createNewGameWithBots(int playersPerTeam, FrameCreator frameCreator) {
        List<Player> playerList = new LinkedList<>();
        for (int i = 0; i < playersPerTeam; i++) {
            playerList.add(
                new Player(false, new ScriptedStrategy(),
                    new TwoDimVector(-Game.RED_SIDE * Game.PITCH_LENGTH/8.0, -128 + (i + 1) * 256.0 / (1 + playersPerTeam)), i+1));
            playerList.add(
                new Player(true, new ScriptedStrategy(),
                    new TwoDimVector(Game.RED_SIDE * Game.PITCH_LENGTH/8.0, -128 + (i + 1) * 256.0 / (1 + playersPerTeam)), i+1));
        }
        return new Game(playerList, new Ball(new TwoDimVector(Game.LEFT_GOAL_CENTER.getX()+2, 0)), frameCreator, false);
    }
}
