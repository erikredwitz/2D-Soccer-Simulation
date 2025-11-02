package model;

import view.GUI;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import static model.Ball.BALL_RADIUS;
import static model.Player.PLAYER_RADIUS;


public class FrameCreator {
    public static final Color PITCH_COLOR = new Color(0, 140, 0);
    public static final Color BALL_COLOR = Color.LIGHT_GRAY;
    public static final double LINE_THICKNESS_WIDTH_RATIO = 0.01;
    public static final int FONT_SIZE_SHIRT_NUMBER = 12;
    private final BufferedImage pitchImage;


    private GUI gui;
    public static final int WIDTH = Game.PITCH_LENGTH;
    @SuppressWarnings("SuspiciousNameCombination")
    public static final int HEIGHT = Game.PITCH_WIDTH;
    public static final int FONT_SIZE_SCORE = 20;

    public FrameCreator(GUI gui) {
        this.gui = gui;
        pitchImage = createImageOfPitch();
    }

    public void createAndSetFrame(Situation situation) {
        var img = createFrame(situation);
        EventQueue.invokeLater(()->gui.setImage(img));
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public synchronized BufferedImage createFrame(Situation situation) {
        BufferedImage bufferedImage = deepCopy(pitchImage);
        for (var x : situation.getPlayers()) {
            drawPlayer(x, bufferedImage);
        }
        drawBall(situation.getBall(), bufferedImage);
        drawScore(bufferedImage, situation.getRedScore(), situation.getBlueScore());
        return bufferedImage;
    }

    private void drawScore(BufferedImage bi, int red, int blue) {
        var graphics = bi.createGraphics();
        graphics.setFont(new Font("Monospaced", Font.PLAIN, FONT_SIZE_SCORE));
        int distanceToEdge = FONT_SIZE_SCORE;
        int redX;
        int blueX;
        int leftX = distanceToEdge;
        int rightX = Game.PITCH_LENGTH - 2 * distanceToEdge;
        if (Game.RED_SIDE < 0) {
            redX = leftX;
            blueX = rightX;
        } else {
            redX = rightX;
            blueX = leftX;
        }
        graphics.setColor(Color.red);
        graphics.drawString(String.valueOf(red), redX, distanceToEdge);
        graphics.setColor(Color.blue);
        graphics.drawString(String.valueOf(blue), blueX, distanceToEdge);
    }

    private BufferedImage createImageOfPitch() {
        int width = WIDTH;
        int height = HEIGHT;
        var b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = b.createGraphics();
        int lineThickness = (int) (LINE_THICKNESS_WIDTH_RATIO * width);
        int goalSize = (int) (Game.GOAL_SIZE_PITCH_WIDTH_RATIO * height);
        g.setColor(PITCH_COLOR);
        g.fillRect(0, 0, width, height);
        //verbotener Bereich
        int forbiddenAreaSize = Game.FORBIDDEN_AREA_SIZE;
        for (int i = 0; i < 2; i++) {
            g.setColor(Color.WHITE);
            g.fillOval(i * width - forbiddenAreaSize / 2 - lineThickness,
                (height - forbiddenAreaSize) / 2 - lineThickness, forbiddenAreaSize + 2 * lineThickness,
                forbiddenAreaSize + 2 * lineThickness);
            g.setColor(PITCH_COLOR);
            g.fillOval(i * width - forbiddenAreaSize / 2, (height - forbiddenAreaSize) / 2, forbiddenAreaSize,
                forbiddenAreaSize);
        }
        //weiße Umrandung
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, lineThickness);
        g.fillRect(width - lineThickness, 0, lineThickness, height);
        g.fillRect(0, height - lineThickness, width, lineThickness);
        g.fillRect(0, 0, lineThickness, height);
        //Tore: Aussparungen in der Umrandung
        g.setColor(PITCH_COLOR);
        g.fillRect(0, (height - goalSize) / 2, lineThickness, goalSize);
        g.fillRect(width - lineThickness, (height - goalSize) / 2, lineThickness, goalSize);
        return b;
    }

    private void drawPlayer(Player x, BufferedImage b) {
        var graphics = b.createGraphics();
        int radius;
        radius = PLAYER_RADIUS;
        if (x.isTeamRed()) {
            graphics.setColor(Color.RED);
        } else {
            graphics.setColor(Color.BLUE);
        }
        var position = x.getPosition();
        int diameter = 2 * radius;
        graphics.fillOval(getXPositionForFrame(position.getX(), b.getWidth()) - radius,
            getYPositionForFrame(position.getY(), b.getHeight()) - radius,
            diameter,
            diameter);

        graphics.setFont(new Font("Arial", Font.BOLD, FONT_SIZE_SHIRT_NUMBER));
        graphics.setColor(x.getMinimalDurationBeforeNextShot() > 0 ? Color.BLACK : Color.LIGHT_GRAY);
        final long sqrtOfRadiusRounded = Math.round(Math.sqrt(radius));
        graphics.drawString(String.valueOf(x.getPlayerId()), getXPositionForFrame(Math.round(position.getX()),b.getWidth()) -
            sqrtOfRadiusRounded, getYPositionForFrame(Math.round(position.getY()), b.getHeight()) + sqrtOfRadiusRounded);
    }

    private void drawBall(Ball x, BufferedImage b) {
        var graphics = b.createGraphics();
        int radius;
        graphics.setColor(BALL_COLOR);
        radius = BALL_RADIUS;
        var position = x.getPosition();
        int diameter = 2 * radius;
        graphics.fillOval(getXPositionForFrame(position.getX(), b.getWidth()) - radius,
            getYPositionForFrame(position.getY(), b.getHeight()) - radius,
            diameter,
            diameter);
    }

    private int getXPositionForFrame(double xPosition, int widthOfFrame) {
        return (int) Math.round(xPosition + (double) (widthOfFrame / 2));
    }

    private int getYPositionForFrame(double yPosition, int heightOfFrame) {
        return (int) Math.round(yPosition + (double) (heightOfFrame / 2));
    }


}
