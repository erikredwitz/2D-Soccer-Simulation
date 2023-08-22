package model;

import view.GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static model.Ball.BALL_RADIUS;
import static model.Player.PLAYER_RADIUS;


public class FrameCreator {
    public static final Color PITCH_COLOR = new Color(0, 140, 0);
    public static final Color BALL_COLOR = Color.WHITE;


    private GUI gui;
    private BufferedImage bufferedImage;
    private Graphics graphics;
    private static final int WIDTH = 512;
    private static final int HEIGHT = 256;

    public FrameCreator(GUI gui) {
        this.gui = gui;
        this.bufferedImage = new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB);
        this.graphics = bufferedImage.createGraphics();
    }

    public void createFrame(List<MovingFrameElement> elements) {
        graphics.setColor(PITCH_COLOR);
        graphics.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        //eventuell unnötig die Liste zu sortieren, der Gedanke war, dass schnellere Spieler über langsamere gemalt werden
        //Und der Ball immer unter den Spielern.
        //Aber vielleicht sollten sich Elemente einfach nicht überlappen dürfen. Das vielleicht noch ändern.
        Collections.sort(elements);
        for (var x : elements) {
            drawElement(x);
        }
        gui.setImage(bufferedImage);
    }

    private void drawElement(MovingFrameElement x) {
        int radius;
        if (x.isBall()) {
            graphics.setColor(BALL_COLOR);
            radius = BALL_RADIUS;
        } else {
            radius = PLAYER_RADIUS;
            if (x.isTeamRed()) {
                graphics.setColor(Color.RED);
            } else {
                graphics.setColor(Color.BLUE);
            }
        }
        var position = x.getPosition();
        int diameter = 2 * radius;
        graphics.fillOval(getXPositionForFrame(position.getX()), getYPositionForFrame(position.getY()), diameter, diameter);
    }
    private int getXPositionForFrame(double xPosition){
        return (int) Math.round(xPosition + (double) bufferedImage.getWidth() / 2);
    }
    private int getYPositionForFrame(double yPosition){
        return (int) Math.round(yPosition + (double) bufferedImage.getHeight() / 2);
    }
}
